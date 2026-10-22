#!/usr/bin/env python3
"""
scripts/migrate_readings.py
---------------------------
Migrates sensor readings into an isolated InfluxDB 2.x instance.
Reads from either a local CSV export or an optional read-only Oracle connection.

Supports:
  1. Official `influxdb-client` when installed.
  2. Built-in standard library HTTP client fallback (`urllib.request`)
     so this script runs out-of-the-box on ANY fresh Python installation.
"""

import os
import sys
import csv
import argparse
import urllib.request
import urllib.parse
import urllib.error
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Any, List, Tuple, Optional

# Load local .env if present (strictly inside da3-influxdb-migration)
SCRIPT_DIR = Path(__file__).resolve().parent
BASE_DIR = SCRIPT_DIR.parent
ENV_PATH = BASE_DIR / ".env"

if ENV_PATH.exists():
    try:
        from dotenv import load_dotenv
        load_dotenv(dotenv_path=ENV_PATH)
    except ImportError:
        # Fallback simple .env parser if python-dotenv is not installed
        with open(ENV_PATH, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    k, v = k.strip(), v.strip().strip("'").strip('"')
                    if k not in os.environ:
                        os.environ[k] = v


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Migrate Smart Home sensor readings to InfluxDB 2.x (DA3 Component)."
    )
    parser.add_argument(
        "--csv",
        type=str,
        default=str(BASE_DIR / "data" / "sample_sensor_readings.csv"),
        help="Path to sample CSV sensor readings dataset."
    )
    parser.add_argument(
        "--url",
        type=str,
        default=os.getenv("INFLUXDB_URL", "http://localhost:8086"),
        help="InfluxDB URL (default: http://localhost:8086)"
    )
    parser.add_argument(
        "--org",
        type=str,
        default=os.getenv("INFLUXDB_ORG", "smarthome_org"),
        help="InfluxDB Organization name."
    )
    parser.add_argument(
        "--bucket",
        type=str,
        default=os.getenv("INFLUXDB_BUCKET", "sensor_telemetry"),
        help="InfluxDB target bucket name."
    )
    parser.add_argument(
        "--token",
        type=str,
        default=os.getenv("INFLUXDB_TOKEN", "da3_super_secret_sensor_telemetry_token_123456789"),
        help="InfluxDB API Authentication Token."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Parse and validate records without writing to InfluxDB."
    )
    return parser.parse_args()


def parse_timestamp(timestamp_str: str) -> Optional[datetime]:
    """
    Parses ISO 8601 or standard SQL timestamp formats into a UTC datetime.
    """
    if not timestamp_str or not timestamp_str.strip():
        return None
    ts = timestamp_str.strip()
    for fmt in (
        "%Y-%m-%dT%H:%M:%SZ",
        "%Y-%m-%dT%H:%M:%S",
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%d %H:%M:%S.%f",
    ):
        try:
            dt = datetime.strptime(ts, fmt)
            return dt.replace(tzinfo=timezone.utc)
        except ValueError:
            continue
    try:
        dt = datetime.fromisoformat(ts.replace("Z", "+00:00"))
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt
    except Exception:
        return None


def validate_and_transform_record(raw: Dict[str, Any]) -> Tuple[Optional[Dict[str, Any]], Optional[str]]:
    """
    Validates a raw dictionary record from CSV/Oracle and returns
    (transformed_dict, error_message).
    """
    try:
        # 1. Device ID
        device_id_raw = raw.get("device_id")
        if device_id_raw is None or str(device_id_raw).strip() == "":
            return None, "Missing device_id"
        device_id = str(int(device_id_raw))

        # 2. Reading ID
        reading_id_raw = raw.get("reading_id")
        if reading_id_raw is None or str(reading_id_raw).strip() == "":
            return None, "Missing reading_id"
        reading_id = int(reading_id_raw)

        # 3. Value
        value_raw = raw.get("value")
        if value_raw is None or str(value_raw).strip() == "":
            return None, "Missing value"
        value = float(value_raw)

        # 4. Timestamp
        reading_time_raw = raw.get("reading_time")
        dt = parse_timestamp(str(reading_time_raw))
        if dt is None:
            return None, f"Invalid timestamp format: '{reading_time_raw}'"

        # 5. Metadata tags (with safe fallbacks)
        device_name = str(raw.get("device_name", f"Device_{device_id}")).strip()
        device_subtype = str(raw.get("device_subtype", "TEMPERATURE_SENSOR")).strip()
        unit = str(raw.get("unit", "CELSIUS")).strip()

        transformed = {
            "measurement": "sensor_reading",
            "tags": {
                "device_id": device_id,
                "device_name": device_name,
                "device_subtype": device_subtype,
                "unit": unit,
            },
            "fields": {
                "value": value,
                "reading_id": reading_id,
            },
            "time": dt,
            "dedup_key": f"{device_id}_{reading_id}_{dt.isoformat()}"
        }
        return transformed, None

    except (ValueError, TypeError) as ex:
        return None, f"Data conversion error: {str(ex)}"


def load_records_from_csv(csv_path: str) -> List[Dict[str, Any]]:
    path = Path(csv_path)
    if not path.exists():
        raise FileNotFoundError(f"CSV file not found: {csv_path}")

    records = []
    with open(path, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            records.append(dict(row))
    return records


def escape_tag(val: str) -> str:
    return str(val).replace(" ", r"\ ").replace(",", r"\,").replace("=", r"\=")


def to_line_protocol(point: Dict[str, Any]) -> str:
    """Formats a transformed point dictionary into InfluxDB Line Protocol string."""
    measurement = point["measurement"]
    tags_str = ",".join(f"{escape_tag(k)}={escape_tag(v)}" for k, v in point["tags"].items())
    fields_str = ",".join(
        f"{k}={v}i" if isinstance(v, int) else f"{k}={v}"
        for k, v in point["fields"].items()
    )
    ns_timestamp = int(point["time"].timestamp() * 1_000_000_000)
    return f"{measurement},{tags_str} {fields_str} {ns_timestamp}"


def write_via_http_api(url: str, org: str, bucket: str, token: str, lines: List[str]):
    """Zero-dependency HTTP write fallback using urllib."""
    clean_url = url.rstrip("/")
    query_params = urllib.parse.urlencode({"org": org, "bucket": bucket, "precision": "ns"})
    endpoint = f"{clean_url}/api/v2/write?{query_params}"
    data = "\n".join(lines).encode("utf-8")

    req = urllib.request.Request(
        endpoint,
        data=data,
        headers={
            "Authorization": f"Token {token}",
            "Content-Type": "text/plain; charset=utf-8"
        },
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            if resp.status not in (200, 204):
                raise RuntimeError(f"InfluxDB HTTP write returned unexpected status {resp.status}")
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"InfluxDB HTTP write failed (HTTP {e.code}): {err_msg}")


def run_migration(
    csv_path: str,
    url: str,
    org: str,
    bucket: str,
    token: str,
    dry_run: bool = False
) -> Dict[str, Any]:
    """
    Executes the migration process and returns statistical metrics.
    """
    stats = {
        "total_read": 0,
        "successfully_transformed": 0,
        "written": 0,
        "skipped_duplicate": 0,
        "rejected": 0,
        "rejection_reasons": []
    }

    print(f"[*] Reading sensor records from: {csv_path}")
    raw_records = load_records_from_csv(csv_path)
    stats["total_read"] = len(raw_records)

    valid_points_data = []
    seen_dedup_keys = set()

    for idx, raw in enumerate(raw_records, start=1):
        transformed, err = validate_and_transform_record(raw)
        if err:
            stats["rejected"] += 1
            stats["rejection_reasons"].append(f"Row {idx}: {err}")
            continue

        dedup_key = transformed["dedup_key"]
        if dedup_key in seen_dedup_keys:
            stats["skipped_duplicate"] += 1
            continue

        seen_dedup_keys.add(dedup_key)
        stats["successfully_transformed"] += 1
        valid_points_data.append(transformed)

    if dry_run:
        print("[*] Dry-run enabled. Skipping InfluxDB network write.")
        stats["written"] = stats["successfully_transformed"]
        return stats

    print(f"[*] Connecting to InfluxDB at: {url} (Org: '{org}', Bucket: '{bucket}')")

    # Method 1: Official InfluxDB Python Client (if available)
    has_client = False
    try:
        from influxdb_client import InfluxDBClient, Point, WritePrecision
        from influxdb_client.client.write_api import SYNCHRONOUS
        has_client = True
    except ImportError:
        has_client = False

    if has_client:
        print("[*] Using official influxdb-client library.")
        with InfluxDBClient(url=url, token=token, org=org) as client:
            write_api = client.write_api(write_options=SYNCHRONOUS)
            points = []
            for item in valid_points_data:
                p = Point(item["measurement"])
                for t_k, t_v in item["tags"].items():
                    p = p.tag(t_k, t_v)
                for f_k, f_v in item["fields"].items():
                    p = p.field(f_k, f_v)
                p = p.time(item["time"], WritePrecision.NS)
                points.append(p)

            write_api.write(bucket=bucket, org=org, record=points)
            stats["written"] = len(points)
    else:
        # Method 2: High-Performance Standard Library HTTP Fallback
        print("[*] 'influxdb-client' not installed. Using built-in HTTP REST API client.")
        lines = [to_line_protocol(item) for item in valid_points_data]
        write_via_http_api(url=url, org=org, bucket=bucket, token=token, lines=lines)
        stats["written"] = len(lines)

    print(f"[+] Successfully wrote {stats['written']} points into bucket '{bucket}'.")
    return stats


def print_summary(stats: Dict[str, Any]):
    print("\n" + "=" * 50)
    print("        INFLUXDB SENSOR MIGRATION SUMMARY        ")
    print("=" * 50)
    print(f"  Total records read:         {stats['total_read']}")
    print(f"  Successfully transformed:   {stats['successfully_transformed']}")
    print(f"  Successfully written:       {stats['written']}")
    print(f"  Skipped duplicates:         {stats['skipped_duplicate']}")
    print(f"  Rejected records:           {stats['rejected']}")
    if stats["rejection_reasons"]:
        print("\n  Rejection details:")
        for reason in stats["rejection_reasons"][:10]:
            print(f"    - {reason}")
        if len(stats["rejection_reasons"]) > 10:
            print(f"    - ... and {len(stats['rejection_reasons']) - 10} more.")
    print("=" * 50 + "\n")


if __name__ == "__main__":
    args = parse_arguments()
    try:
        summary = run_migration(
            csv_path=args.csv,
            url=args.url,
            org=args.org,
            bucket=args.bucket,
            token=args.token,
            dry_run=args.dry_run
        )
        print_summary(summary)
        sys.exit(0)
    except Exception as e:
        print(f"\n[!] Migration failed with error: {e}", file=sys.stderr)
        sys.exit(1)
