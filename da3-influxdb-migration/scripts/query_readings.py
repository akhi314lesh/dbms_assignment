#!/usr/bin/env python3
"""
scripts/query_readings.py
-------------------------
Executes three core analytical time-series queries against InfluxDB 2.x using Flux:
  1. Latest reading: Retrieve the most recent measurement for a specific sensor.
  2. Average temperature: Calculate mean temperature for a sensor/group over a time window.
  3. Reading count: Count total measurements recorded within a specified time window.

Supports:
  1. Official `influxdb-client` when installed.
  2. Built-in standard library HTTP client fallback (`urllib.request`)
     so queries run out-of-the-box on ANY fresh Python installation.
"""

import os
import sys
import csv
import io
import argparse
import urllib.request
import urllib.parse
import urllib.error
from pathlib import Path
from typing import Dict, Any, List, Optional

# Load local .env if present (strictly inside da3-influxdb-migration)
SCRIPT_DIR = Path(__file__).resolve().parent
BASE_DIR = SCRIPT_DIR.parent
ENV_PATH = BASE_DIR / ".env"

if ENV_PATH.exists():
    try:
        from dotenv import load_dotenv
        load_dotenv(dotenv_path=ENV_PATH)
    except ImportError:
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
        description="Query sensor telemetry from InfluxDB 2.x (DA3 Component)."
    )
    parser.add_argument(
        "--query",
        choices=["latest", "avg", "count", "all"],
        default="all",
        help="Query to execute: 'latest', 'avg', 'count', or 'all' (default: 'all')."
    )
    parser.add_argument(
        "--device",
        type=str,
        default="4",
        help="Device ID to query (default: '4' - Kitchen Temp Sensor)."
    )
    parser.add_argument(
        "--start",
        type=str,
        default="2026-09-17T00:00:00Z",
        help="Start timestamp for query window in ISO format (default: '2026-09-17T00:00:00Z')."
    )
    parser.add_argument(
        "--stop",
        type=str,
        default="2026-09-18T00:00:00Z",
        help="Stop timestamp for query window in ISO format (default: '2026-09-18T00:00:00Z')."
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
        help="InfluxDB bucket name."
    )
    parser.add_argument(
        "--token",
        type=str,
        default=os.getenv("INFLUXDB_TOKEN", "da3_super_secret_sensor_telemetry_token_123456789"),
        help="InfluxDB API Authentication Token."
    )
    return parser.parse_args()


# -------------------------------------------------------------------------
# FLUX QUERY BUILDERS (InfluxDB 2.x Native Query Language)
# -------------------------------------------------------------------------

def build_latest_reading_query(bucket: str, device_id: str, start: str, stop: str) -> str:
    """
    Query 1: Retrieves the latest temperature measurement for a specified device.
    Uses Flux `last()` selector on the `value` field.
    """
    return f'''
    from(bucket: "{bucket}")
      |> range(start: {start}, stop: {stop})
      |> filter(fn: (r) => r._measurement == "sensor_reading")
      |> filter(fn: (r) => r.device_id == "{device_id}")
      |> filter(fn: (r) => r._field == "value")
      |> last()
    '''


def build_average_temperature_query(bucket: str, device_id: Optional[str], start: str, stop: str) -> str:
    """
    Query 2: Computes the mean temperature across a time window.
    Strictly filters for `unit == "CELSIUS"` and `device_subtype == "TEMPERATURE_SENSOR"`
    so incompatible measurement types are never averaged together.
    """
    device_filter = f'|> filter(fn: (r) => r.device_id == "{device_id}")' if device_id else ""
    return f'''
    from(bucket: "{bucket}")
      |> range(start: {start}, stop: {stop})
      |> filter(fn: (r) => r._measurement == "sensor_reading")
      |> filter(fn: (r) => r.device_subtype == "TEMPERATURE_SENSOR")
      |> filter(fn: (r) => r.unit == "CELSIUS")
      |> filter(fn: (r) => r._field == "value")
      {device_filter}
      |> mean()
    '''


def build_reading_count_query(bucket: str, device_id: Optional[str], start: str, stop: str) -> str:
    """
    Query 3: Counts total reading events in the specified time range.
    Uses Flux `count()` aggregator on the `value` field.
    """
    device_filter = f'|> filter(fn: (r) => r.device_id == "{device_id}")' if device_id else ""
    return f'''
    from(bucket: "{bucket}")
      |> range(start: {start}, stop: {stop})
      |> filter(fn: (r) => r._measurement == "sensor_reading")
      |> filter(fn: (r) => r._field == "value")
      {device_filter}
      |> count()
    '''


# -------------------------------------------------------------------------
# EXECUTION & QUERY ENGINES
# -------------------------------------------------------------------------

def query_via_http_api(url: str, org: str, token: str, query_flux: str) -> List[Dict[str, Any]]:
    """Executes a Flux query against InfluxDB HTTP API and parses annotated CSV."""
    clean_url = url.rstrip("/")
    endpoint = f"{clean_url}/api/v2/query?org={urllib.parse.quote(org)}"
    req = urllib.request.Request(
        endpoint,
        data=query_flux.strip().encode("utf-8"),
        headers={
            "Authorization": f"Token {token}",
            "Content-Type": "application/vnd.flux",
            "Accept": "application/csv"
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            content = resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"InfluxDB HTTP query failed (HTTP {e.code}): {err_msg}")

    # Parse InfluxDB Annotated CSV
    results = []
    headers = []
    for line in content.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        reader = list(csv.reader(io.StringIO(line)))
        if not reader:
            continue
        row = reader[0]
        if "_value" in row:
            headers = row
            continue
        if headers and len(row) == len(headers):
            row_dict = dict(zip(headers, row))
            results.append({
                "time": row_dict.get("_time"),
                "field": row_dict.get("_field"),
                "value": row_dict.get("_value"),
                "device_id": row_dict.get("device_id"),
                "device_name": row_dict.get("device_name"),
                "device_subtype": row_dict.get("device_subtype"),
                "unit": row_dict.get("unit"),
            })
    return results


def execute_flux(url: str, org: str, token: str, query_flux: str) -> List[Dict[str, Any]]:
    try:
        from influxdb_client import InfluxDBClient
        with InfluxDBClient(url=url, token=token, org=org) as client:
            query_api = client.query_api()
            tables = query_api.query(query_flux)
            results = []
            for table in tables:
                for record in table.records:
                    results.append({
                        "time": str(record.get_time()),
                        "field": record.get_field(),
                        "value": record.get_value(),
                        "device_id": record.values.get("device_id"),
                        "device_name": record.values.get("device_name"),
                        "device_subtype": record.values.get("device_subtype"),
                        "unit": record.values.get("unit"),
                    })
            return results
    except ImportError:
        return query_via_http_api(url, org, token, query_flux)


def run_latest_reading(url: str, org: str, token: str, bucket: str, device_id: str, start: str, stop: str):
    query_flux = build_latest_reading_query(bucket, device_id, start, stop)
    rows = execute_flux(url, org, token, query_flux)
    print(f"\n--- [QUERY 1] Latest Reading for Device {device_id} ---")
    if not rows:
        print(f"  No readings found for Device {device_id} in range [{start} to {stop}].")
        return None
    r = rows[0]
    print(f"  Device ID:    {r.get('device_id')}")
    print(f"  Device Name:  {r.get('device_name')}")
    print(f"  Timestamp:    {r.get('time')}")
    print(f"  Value:        {r.get('value')} {r.get('unit')}")
    return r


def run_average_temperature(url: str, org: str, token: str, bucket: str, device_id: Optional[str], start: str, stop: str):
    query_flux = build_average_temperature_query(bucket, device_id, start, stop)
    rows = execute_flux(url, org, token, query_flux)
    scope = f"Device {device_id}" if device_id else "All Temperature Sensors"
    print(f"\n--- [QUERY 2] Average Temperature for {scope} ---")
    if not rows:
        print(f"  No temperature records found in range [{start} to {stop}].")
        return None
    r = rows[0]
    avg_val = round(float(r.get('value', 0.0)), 2)
    print(f"  Target Scope: {scope}")
    print(f"  Unit:         {r.get('unit', 'CELSIUS')}")
    print(f"  Window:       {start} -> {stop}")
    print(f"  Average Temp: {avg_val}°C")
    return avg_val


def run_reading_count(url: str, org: str, token: str, bucket: str, device_id: Optional[str], start: str, stop: str):
    query_flux = build_reading_count_query(bucket, device_id, start, stop)
    rows = execute_flux(url, org, token, query_flux)
    scope = f"Device {device_id}" if device_id else "All Sensors"
    print(f"\n--- [QUERY 3] Reading Count for {scope} ---")
    if not rows:
        print(f"  Zero records found in range [{start} to {stop}].")
        return 0
    r = rows[0]
    count_val = int(r.get('value', 0))
    print(f"  Target Scope: {scope}")
    print(f"  Window:       {start} -> {stop}")
    print(f"  Total Count:  {count_val} readings")
    return count_val


def main():
    args = parse_arguments()
    print(f"[*] Querying InfluxDB at {args.url} (Org: '{args.org}', Bucket: '{args.bucket}')")

    try:
        if args.query in ("latest", "all"):
            run_latest_reading(args.url, args.org, args.token, args.bucket, args.device, args.start, args.stop)

        if args.query in ("avg", "all"):
            run_average_temperature(args.url, args.org, args.token, args.bucket, args.device, args.start, args.stop)

        if args.query in ("count", "all"):
            run_reading_count(args.url, args.org, args.token, args.bucket, args.device, args.start, args.stop)
            if args.query == "all":
                run_reading_count(args.url, args.org, args.token, args.bucket, None, args.start, args.stop)

        print("\n[+] Query execution completed successfully.\n")
    except Exception as e:
        print(f"\n[!] Query execution failed: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
