#!/usr/bin/env python3
"""
tests/test_sensor_migration.py
------------------------------
Automated validation test suite for DA3 InfluxDB Sensor Migration.

Covers:
  1. Sample CSV loading and schema validation.
  2. Data transformation (types, tags, fields, ISO timestamps).
  3. Rejection of invalid records (bad timestamps, missing fields, non-numeric values).
  4. Idempotency and duplicate detection.
  5. Mathematical verification of query aggregations (deterministic expected answers).
  6. Live InfluxDB integration tests (conditional upon running InfluxDB server).

Run with:
  pytest tests/test_sensor_migration.py -v
  or:
  python tests/test_sensor_migration.py
"""

import os
import sys
import unittest
from pathlib import Path
from datetime import datetime, timezone

# Ensure scripts package is on python path
TEST_DIR = Path(__file__).resolve().parent
BASE_DIR = TEST_DIR.parent
SCRIPTS_DIR = BASE_DIR / "scripts"
sys.path.insert(0, str(SCRIPTS_DIR))

from migrate_readings import (
    load_records_from_csv,
    validate_and_transform_record,
    parse_timestamp,
    run_migration
)
from query_readings import (
    build_latest_reading_query,
    build_average_temperature_query,
    build_reading_count_query
)


class TestSensorMigrationUnit(unittest.TestCase):
    """
    Pure Unit Tests: Validates data transformation, parsing, rejection,
    deduplication, and mathematical expectations without external database dependencies.
    """

    def setUp(self):
        self.csv_path = BASE_DIR / "data" / "sample_sensor_readings.csv"

    def test_csv_file_exists_and_loads(self):
        """Verify sample CSV dataset exists and contains expected 60 records."""
        records = load_records_from_csv(str(self.csv_path))
        self.assertGreaterEqual(len(records), 48)
        self.assertEqual(len(records), 60)

        # Verify required headers
        required_cols = {"device_id", "reading_id", "value", "reading_time", "device_name", "device_subtype", "unit"}
        self.assertTrue(required_cols.issubset(records[0].keys()))

    def test_record_transformation_valid(self):
        """Verify valid record is transformed into correct InfluxDB measurement, tags, and fields."""
        raw = {
            "device_id": "4",
            "reading_id": "1",
            "value": "23.40",
            "reading_time": "2026-09-17T08:00:00Z",
            "device_name": "Kitchen Temp Sensor",
            "device_subtype": "TEMPERATURE_SENSOR",
            "unit": "CELSIUS"
        }
        transformed, err = validate_and_transform_record(raw)
        self.assertIsNone(err)
        self.assertIsNotNone(transformed)
        self.assertEqual(transformed["measurement"], "sensor_reading")
        self.assertEqual(transformed["tags"]["device_id"], "4")
        self.assertEqual(transformed["tags"]["unit"], "CELSIUS")
        self.assertEqual(transformed["tags"]["device_subtype"], "TEMPERATURE_SENSOR")
        self.assertEqual(transformed["fields"]["value"], 23.40)
        self.assertEqual(transformed["fields"]["reading_id"], 1)
        self.assertEqual(transformed["time"], datetime(2026, 9, 17, 8, 0, 0, tzinfo=timezone.utc))

    def test_rejection_invalid_value(self):
        """Verify records with non-numeric value are cleanly rejected."""
        raw = {
            "device_id": "4",
            "reading_id": "99",
            "value": "INVALID_FLOAT",
            "reading_time": "2026-09-17T08:00:00Z"
        }
        transformed, err = validate_and_transform_record(raw)
        self.assertIsNone(transformed)
        self.assertIn("Data conversion error", err)

    def test_rejection_missing_device_id(self):
        """Verify records missing device_id are rejected."""
        raw = {
            "device_id": "",
            "reading_id": "99",
            "value": "24.50",
            "reading_time": "2026-09-17T08:00:00Z"
        }
        transformed, err = validate_and_transform_record(raw)
        self.assertIsNone(transformed)
        self.assertIn("Missing device_id", err)

    def test_rejection_invalid_timestamp(self):
        """Verify records with unparseable timestamps are rejected."""
        raw = {
            "device_id": "4",
            "reading_id": "99",
            "value": "24.50",
            "reading_time": "Not-A-Timestamp"
        }
        transformed, err = validate_and_transform_record(raw)
        self.assertIsNone(transformed)
        self.assertIn("Invalid timestamp format", err)

    def test_idempotency_deduplication_logic(self):
        """Verify that duplicate rows with identical (device_id, reading_id, timestamp) are identified."""
        raw1 = {
            "device_id": "4",
            "reading_id": "1",
            "value": "23.40",
            "reading_time": "2026-09-17T08:00:00Z"
        }
        t1, _ = validate_and_transform_record(raw1)
        t2, _ = validate_and_transform_record(raw1)
        self.assertEqual(t1["dedup_key"], t2["dedup_key"])

    def test_deterministic_device_4_math(self):
        """
        Verify known ground-truth calculations for Device 4 (Kitchen Temp Sensor):
        Values: 23.4, 23.8, 24.1, 24.5, 25.2, 26.8, 26.1, 25.4, 24.9, 24.3, 23.9, 23.5
        Count = 12
        Sum = 295.9
        Average = 24.65833... -> 24.66
        Latest = 23.50 (at 19:00:00)
        """
        records = load_records_from_csv(str(self.csv_path))
        d4_records = [r for r in records if r["device_id"] == "4"]
        self.assertEqual(len(d4_records), 12)

        values = [float(r["value"]) for r in d4_records]
        expected_values = [23.40, 23.80, 24.10, 24.50, 25.20, 26.80, 26.10, 25.40, 24.90, 24.30, 23.90, 23.50]
        self.assertEqual(values, expected_values)

        calculated_avg = round(sum(values) / len(values), 2)
        self.assertEqual(calculated_avg, 24.66)

        # Check chronological latest
        sorted_d4 = sorted(d4_records, key=lambda x: x["reading_time"])
        latest = sorted_d4[-1]
        self.assertEqual(float(latest["value"]), 23.50)
        self.assertEqual(latest["reading_time"], "2026-09-17T19:00:00Z")

    def test_flux_query_builders(self):
        """Verify Flux query strings contain proper bucket, measurement, tags, and function syntax."""
        q1 = build_latest_reading_query("sensor_telemetry", "4", "2026-09-17T00:00:00Z", "2026-09-18T00:00:00Z")
        self.assertIn('from(bucket: "sensor_telemetry")', q1)
        self.assertIn('r.device_id == "4"', q1)
        self.assertIn('last()', q1)

        q2 = build_average_temperature_query("sensor_telemetry", "4", "2026-09-17T00:00:00Z", "2026-09-18T00:00:00Z")
        self.assertIn('r.unit == "CELSIUS"', q2)
        self.assertIn('r.device_subtype == "TEMPERATURE_SENSOR"', q2)
        self.assertIn('mean()', q2)

        q3 = build_reading_count_query("sensor_telemetry", "4", "2026-09-17T00:00:00Z", "2026-09-18T00:00:00Z")
        self.assertIn('count()', q3)


class TestSensorMigrationLiveIntegration(unittest.TestCase):
    """
    Live Integration Tests against InfluxDB 2.x Docker container.
    Gracefully skips if InfluxDB is not running or unreachable on port 8086.
    """

    def setUp(self):
        self.url = os.getenv("INFLUXDB_URL", "http://localhost:8086")
        self.token = os.getenv("INFLUXDB_TOKEN", "da3_super_secret_sensor_telemetry_token_123456789")
        self.org = os.getenv("INFLUXDB_ORG", "smarthome_org")
        self.bucket = os.getenv("INFLUXDB_BUCKET", "sensor_telemetry")
        self.csv_path = str(BASE_DIR / "data" / "sample_sensor_readings.csv")

        # Test reachability
        self.influx_available = False
        try:
            import urllib.request
            req = urllib.request.Request(f"{self.url}/health")
            with urllib.request.urlopen(req, timeout=1.5) as resp:
                if resp.status == 200:
                    self.influx_available = True
        except Exception:
            self.influx_available = False

    def test_live_migration_and_query_flow(self):
        """Execute end-to-end migration and verify queries against live InfluxDB."""
        if not self.influx_available:
            self.skipTest(f"InfluxDB is not currently reachable at {self.url}. Skipping live integration test.")

        # 1. Run migration
        stats = run_migration(
            csv_path=self.csv_path,
            url=self.url,
            org=self.org,
            bucket=self.bucket,
            token=self.token,
            dry_run=False
        )
        self.assertEqual(stats["total_read"], 60)
        self.assertEqual(stats["successfully_transformed"], 60)
        self.assertEqual(stats["written"], 60)
        self.assertEqual(stats["rejected"], 0)

        # 2. Test re-run idempotency
        stats_rerun = run_migration(
            csv_path=self.csv_path,
            url=self.url,
            org=self.org,
            bucket=self.bucket,
            token=self.token,
            dry_run=False
        )
        self.assertEqual(stats_rerun["written"], 60)

        # 3. Test queries against live InfluxDB
        from query_readings import (
            run_latest_reading,
            run_average_temperature,
            run_reading_count
        )

        # Query 1: Latest Reading
        latest = run_latest_reading(self.url, self.org, self.token, self.bucket, "4", "2026-09-17T00:00:00Z", "2026-09-18T00:00:00Z")
        self.assertIsNotNone(latest)
        self.assertEqual(float(latest["value"]), 23.50)

        # Query 2: Average Temperature
        avg_temp = run_average_temperature(self.url, self.org, self.token, self.bucket, "4", "2026-09-17T00:00:00Z", "2026-09-18T00:00:00Z")
        self.assertIsNotNone(avg_temp)
        self.assertAlmostEqual(avg_temp, 24.66, places=2)

        # Query 3: Reading Count
        count = run_reading_count(self.url, self.org, self.token, self.bucket, "4", "2026-09-17T00:00:00Z", "2026-09-18T00:00:00Z")
        self.assertEqual(count, 12)


if __name__ == "__main__":
    unittest.main(verbosity=2)
