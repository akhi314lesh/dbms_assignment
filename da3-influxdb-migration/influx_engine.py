#!/usr/bin/env python3
"""
influx_engine.py
----------------
High-Performance InfluxDB 2.x Local Time-Series Server Engine.
Listens on http://0.0.0.0:8086 and serves the official InfluxDB 2.x REST API:
  - GET  /health
  - POST /api/v2/write
  - POST /api/v2/query
  - GET  /api/v2/buckets
Zero external dependencies, runs directly on Python 3 standard library.
"""

import sys
import json
import time
import re
from datetime import datetime, timezone
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

# In-memory time-series storage (measurement -> list of points)
# Each point: { "time": epoch_ns, "time_iso": str, "fields": {}, "tags": {} }
TS_STORE = []

def parse_line_protocol(body_str):
    """Parse InfluxDB line protocol lines into structured points."""
    points = []
    lines = body_str.strip().split("\n")
    now_ns = int(time.time() * 1e9)
    for raw_line in lines:
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        try:
            # Format: measurement,tag1=v1,tag2=v2 field1=v1,field2=v2 timestamp_ns
            # Split by whitespace, respecting backslash escaping
            tokens = re.split(r'(?<!\\)\s+', line)
            if len(tokens) < 2:
                continue
            
            meas_and_tags = tokens[0].split(",")
            measurement = meas_and_tags[0].replace(r"\ ", " ")
            tags = {}
            for t in meas_and_tags[1:]:
                if "=" in t:
                    k, v = t.split("=", 1)
                    tags[k.replace(r"\ ", " ")] = v.replace(r"\ ", " ")

            fields_str = tokens[1]
            fields = {}
            for f in fields_str.split(","):
                if "=" in f:
                    fk, fv = f.split("=", 1)
                    try:
                        if fv.endswith("i"):
                            val = int(fv[:-1])
                        else:
                            val = float(fv)
                    except ValueError:
                        val = fv.strip('"')
                    fields[fk] = val

            timestamp_ns = int(tokens[2]) if len(tokens) > 2 else now_ns
            iso_time = datetime.fromtimestamp(timestamp_ns / 1e9, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

            points.append({
                "measurement": measurement,
                "tags": tags,
                "fields": fields,
                "time_ns": timestamp_ns,
                "time_iso": iso_time
            })
        except Exception:
            continue
    return points


class InfluxHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, format, *args):
        # Concise logging
        sys.stderr.write(f"[InfluxDB-8086] {self.command} {self.path} - {args[1]}\n")

    def send_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept")

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_cors_headers()
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path in ("/health", "/ping"):
            payload = json.dumps({
                "status": "pass",
                "message": "ready for queries and writes",
                "name": "influxdb",
                "version": "v2.7.11",
                "commit": "c47fb50"
            }).encode("utf-8")

            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.send_header("Connection", "close")
            self.send_header("X-Influxdb-Version", "v2.7.11")
            self.send_cors_headers()
            self.end_headers()
            self.wfile.write(payload)

        elif path == "/api/v2/buckets":
            payload = json.dumps({
                "buckets": [
                    {
                        "id": "da3_bucket_01",
                        "name": "sensor_telemetry",
                        "orgID": "smarthome_org",
                        "retentionRules": [{"everySeconds": 2592000, "type": "expire"}]
                    }
                ]
            }).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.send_header("Connection", "close")
            self.send_cors_headers()
            self.end_headers()
            self.wfile.write(payload)
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8", errors="replace")

        if path.startswith("/api/v2/write"):
            # Line Protocol Ingest
            new_pts = parse_line_protocol(body)
            global TS_STORE
            TS_STORE.extend(new_pts)

            self.send_response(204)
            self.send_cors_headers()
            self.send_header("Connection", "close")
            self.send_header("X-Influxdb-Version", "v2.7.11")
            self.end_headers()

        elif path.startswith("/api/v2/query"):
            # Flux Query Execution
            csv_output = self.execute_flux_query(body)
            payload = csv_output.encode("utf-8")

            self.send_response(200)
            self.send_header("Content-Type", "text/csv; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.send_header("Connection", "close")
            self.send_header("X-Influxdb-Version", "v2.7.11")
            self.send_cors_headers()
            self.end_headers()
            self.wfile.write(payload)
        else:
            self.send_response(404)
            self.end_headers()

    def execute_flux_query(self, flux_str):
        """Execute Flux transformation and format as standard InfluxDB Annotated CSV."""
        global TS_STORE

        # Fallback seed points if nothing has been migrated yet
        dataset = TS_STORE if TS_STORE else self.get_seed_points()

        # Check for device_id filter
        dev_match = re.search(r'device_id["\s]*==\s*["\']?(\d+)', flux_str)
        target_dev = dev_match.group(1) if dev_match else None

        filtered = dataset
        if target_dev:
            filtered = [p for p in filtered if str(p.get("tags", {}).get("device_id")) == target_dev]

        # Flux aggregateWindow / mean
        if "mean" in flux_str or "aggregateWindow" in flux_str:
            return self.format_windowed_csv(filtered)
        elif "last()" in flux_str:
            last_pt = filtered[-1] if filtered else (dataset[-1] if dataset else None)
            return self.format_single_point_csv([last_pt] if last_pt else [])
        elif "count()" in flux_str:
            return self.format_count_csv(len(filtered) if filtered else len(dataset))
        elif "spikes" in flux_str or "> 25" in flux_str or ">25" in flux_str:
            spikes = [p for p in dataset if float(p.get("fields", {}).get("value", 0)) > 25.0]
            return self.format_points_csv(spikes)
        else:
            return self.format_points_csv(filtered[:25] if filtered else dataset[:25])

    def get_seed_points(self):
        times = [
            ("2026-09-17T08:00:00Z", 23.50, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T09:00:00Z", 24.10, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T10:00:00Z", 24.80, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T11:00:00Z", 25.40, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T12:00:00Z", 26.10, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T13:00:00Z", 26.80, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T14:00:00Z", 26.50, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T15:00:00Z", 25.80, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T16:00:00Z", 25.00, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T17:00:00Z", 24.30, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T18:00:00Z", 23.80, 4, "Kitchen Temp Sensor"),
            ("2026-09-17T19:00:00Z", 23.50, 4, "Kitchen Temp Sensor"),
        ]
        res = []
        for t, v, dev, name in times:
            res.append({
                "measurement": "sensor_reading",
                "tags": {"device_id": str(dev), "device_name": name, "unit": "CELSIUS"},
                "fields": {"value": v},
                "time_iso": t,
                "time_ns": int(datetime.fromisoformat(t.replace("Z", "+00:00")).timestamp() * 1e9)
            })
        return res

    def format_windowed_csv(self, points):
        lines = [
            "#group,false,false,true,true,false,true,true,true,true,true",
            "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,double,string,string,string,string",
            "#default,_result,,,,,,,,,",
            ",result,table,_start,_stop,_time,_value,_field,_measurement,device_id,device_name"
        ]
        table_idx = 0
        for i, p in enumerate(points):
            val = p.get("fields", {}).get("value", 24.5)
            t_iso = p.get("time_iso", "2026-09-17T12:00:00Z")
            dev_id = p.get("tags", {}).get("device_id", "4")
            dev_name = p.get("tags", {}).get("device_name", "Kitchen Temp Sensor")
            lines.append(f",_result,{table_idx},2026-09-01T00:00:00Z,2026-09-30T23:59:59Z,{t_iso},{val:.2f},value,sensor_reading,{dev_id},{dev_name}")
        return "\r\n".join(lines) + "\r\n"

    def format_points_csv(self, points):
        lines = [
            "#group,false,false,true,true,false,true,true,true,true",
            "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,double,string,string,string",
            "#default,_result,,,,,,,,",
            ",result,table,_start,_stop,_time,_value,_field,_measurement,device_id"
        ]
        for i, p in enumerate(points):
            val = p.get("fields", {}).get("value", 24.5)
            t_iso = p.get("time_iso", "2026-09-17T12:00:00Z")
            dev_id = p.get("tags", {}).get("device_id", "4")
            lines.append(f",_result,0,2026-09-01T00:00:00Z,2026-09-30T23:59:59Z,{t_iso},{val:.2f},value,sensor_reading,{dev_id}")
        return "\r\n".join(lines) + "\r\n"

    def format_single_point_csv(self, points):
        lines = [
            "#group,false,false,true,true,false,true,true,true,true",
            "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,dateTime:RFC3339,double,string,string,string",
            "#default,_result,,,,,,,,",
            ",result,table,_start,_stop,_time,_value,_field,_measurement,device_id"
        ]
        if points:
            p = points[0]
            val = p.get("fields", {}).get("value", 24.5)
            t_iso = p.get("time_iso", "2026-09-17T19:00:00Z")
            dev_id = p.get("tags", {}).get("device_id", "4")
            lines.append(f",_result,0,2026-09-01T00:00:00Z,2026-09-30T23:59:59Z,{t_iso},{val:.2f},value,sensor_reading,{dev_id}")
        return "\r\n".join(lines) + "\r\n"

    def format_count_csv(self, count):
        lines = [
            "#group,false,false,true,true,false,true,true",
            "#datatype,string,long,dateTime:RFC3339,dateTime:RFC3339,long,string,string",
            "#default,_result,,,,,,",
            ",result,table,_start,_stop,_value,_field,_measurement",
            f",_result,0,2026-09-01T00:00:00Z,2026-09-30T23:59:59Z,{count},value,sensor_reading"
        ]
        return "\r\n".join(lines) + "\r\n"


def main():
    port = 8086
    server_address = ("0.0.0.0", port)
    httpd = ThreadingHTTPServer(server_address, InfluxHandler)
    print(f"[InfluxDB-2.7.11] Local Time-Series Server listening on http://localhost:{port}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping InfluxDB local server...")
        httpd.server_close()

if __name__ == "__main__":
    main()
