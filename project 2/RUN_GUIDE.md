# DA3 — How to run everything locally (Windows)

Four things run on your machine:

| Port | What | Folder |
|---|---|---|
| 1521 | Oracle Database 21c XE (schema `SMART_HOME`) | installed on Windows |
| 8086 | InfluxDB 2.7 (bucket `sensor_telemetry`) | `da3-influxdb-migration/` (Docker) |
| 8080 | Spring Boot backend (talks to both databases) | `backend/` |
| 5173 / 5174 / 5175 | Oracle site · InfluxDB site · **Comparison dashboard** | `frontend/` · `frontend-influx/` · `frontend-compare/` |

## 0. Install once

- **JDK 21**: `java -version` must print 21.
- **Node.js 18+** (20 or 22 recommended): `node -v`.
- **Docker Desktop**: for InfluxDB.
- **Oracle Database 21c XE**: listening on `localhost:1521`, pluggable database `XEPDB1`.

## 1. Oracle: create the schema (first time only)

Open a terminal in the `database` folder:

```powershell
# a) create the SMART_HOME user (skip if you already have it)
sqlplus system/<your-system-password>@localhost:1521/XEPDB1 @00_create_user.sql

# b) create tables, constraints, sequences, sample data, PL/SQL, auth columns, and sync the sequences
sqlplus SMART_HOME/SmartHomeDev123@localhost:1521/XEPDB1 @setup_all.sql
```

At the end it prints the row count of each of the 17 tables.

> Already set up the schema earlier using the old README? Then just run `@09_sync_sequences.sql`
> as SMART_HOME. Without it, "Create Home" on the website fails with ORA-00001.
> Do **not** run `04b_expand_sample_data.sql`. `04_sample_data.sql` already contains everything.

## 2. Start everything

From the project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

The script:
1. starts InfluxDB in Docker and waits until it is healthy;
2. runs `npm install` in the three website folders (first run only);
3. starts the backend with your Oracle password (it asks; press Enter for `SmartHomeDev123`);
4. starts the three websites;
5. opens **http://localhost:5175** once the backend reports both databases.

The first backend start downloads Maven dependencies, so it takes a few minutes.

### Starting things by hand instead

```powershell
cd da3-influxdb-migration; docker compose up -d; cd ..
cd backend; $env:DB_PASSWORD="SmartHomeDev123"; .\mvnw.cmd spring-boot:run      # terminal 1
cd frontend;          npm install; npm run dev                                   # terminal 2 → :5173
cd frontend-influx;   npm install; npm run dev                                   # terminal 3 → :5174
cd frontend-compare;  npm install; npm run dev                                   # terminal 4 → :5175
```

## 3. Using the comparison dashboard (http://localhost:5175)

1. Check the status pills at the top. **Oracle Connected** and **InfluxDB Connected** must both be green.
   If one is red, the page tells you why (wrong password, Docker not running, …).
2. **01 · Benchmark Dataset**: choose a size and click **Generate benchmark dataset**. The backend
   generates readings for all 18 temperature and motion sensors in the `DEVICES` table and writes the *same*
   rows to Oracle (`SENSOR_BENCHMARK` table, created automatically) and InfluxDB (`sensor_benchmark`
   measurement). The ingestion time of each database is shown straight away.
3. **02 · Choose a Workload**: pick one of the 8 workloads, choose a device or time window, then click **Run comparison**.
   Or click **Run all workloads** to benchmark all of them at once.
4. Read the results:
   - **03** head-to-head bars, the winner, ×-speed-up and **time difference in ms**;
   - **04** key metrics;
   - **05** charts: latency of every run, latency distribution, and the actual query result from both databases plotted together;
   - **06** a plain-English explanation of *why* that database won, plus a statistics table;
   - **07** result verification: proof that both databases returned the same answer;
   - **08** the exact SQL and Flux that were executed;
   - **09** all-workloads charts and summary, **10** history, **11** log, **12** capability matrix;
   - **13** storage efficiency (*Measure storage*: disk space and bytes per reading in each database);
   - **14** scalability test (regenerates Small → Huge, measures load time, storage and all 8 workloads at each size);
   - **15** one chart per evaluation parameter (all 12), **16** conclusion.

A bigger dataset (Extra large / Huge, up to 5 million rows) shows the gap between the engines more clearly: on this
laptop Oracle wins every read up to ~36K readings, the two are level around 156K, and from ~0.8M InfluxDB wins most
reads (5.5× faster in total at 4.67M) and stores the data ~16× smaller.

### Quick backend self-test

With the backend running:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-compare.ps1
```

It checks health, generates a small dataset, runs all 8 workloads, and prints `ALL CHECKS PASSED` when
every workload ran on both databases and returned matching results.

## 4. Comparison API (T1)

| Method | Path | Body / result |
|---|---|---|
| GET | `/api/compare/health` | `{ oracle, influx, oracleVersion, influxVersion, oracleError?, influxError? }` |
| GET | `/api/compare/queries` | the 8 workloads with descriptions and explanations |
| GET | `/api/compare/devices` | sensor devices in the dataset |
| GET | `/api/compare/dataset` | row counts in Oracle and InfluxDB, and `inSync` |
| GET | `/api/compare/storage?settle=true` | disk space of the dataset: Oracle segments, InfluxDB TSM + index + WAL |
| POST | `/api/compare/seed` | `{ days, intervalMinutes }` → identical data in both, with write timings |
| POST | `/api/compare/run` | `{ queryType, iterations, deviceId?, rangeHours?, threshold?, writeRows? }` |
| POST | `/api/compare/run-all` | `{ iterations, writeRows? }` |
| GET / DELETE | `/api/compare/history` | recent runs |

`queryType`: `RAW_RANGE`, `WINDOW_AGGREGATE`, `DEVICE_SUMMARY`, `LATEST_PER_DEVICE`, `THRESHOLD_ALERTS`,
`DAILY_DOWNSAMPLE`, `GLOBAL_AGGREGATE`, `BULK_INSERT`. The old names `range_scan`, `device_agg`,
`windowed_avg`, `anomaly_detect`, `top_n`, `MULTI_DEVICE_GROUP` and `FULL_SCAN_AGGREGATE` are still accepted.

Each `/run` result contains `oracleMs`, `influxMs`, `timeDifferenceMs`, `speedupFactor`, `winner`, per-run
samples with min / median / p95 / max / std-dev, row counts, `resultComparison` (MATCH / PARTIAL / MISMATCH
with side-by-side sample rows), chart data, both executed queries, and a one-paragraph summary.

**How fairness is ensured:** same data in both databases; equivalent SQL and Flux over the same half-open time
window `[start, stop)`, aligned to whole hours or days; one untimed warm-up per side; the run order alternates
between databases; times are measured end-to-end in the backend (send query → all rows received); Oracle uses
a JDBC fetch size of 1000 and fresh optimizer statistics. No simulated numbers. If a database is down, its side
reports the error.

## 5. What was fixed in this hand-over

- **Comparison backend rebuilt.** The old `/api/compare/run` queried a table called `sensor_reading`, which does not exist
  (the real one is `SENSOR_READINGS`). It hid the error, so every Oracle time was meaningless.
  `/api/compare/health` always reported InfluxDB as up. The dashboard silently showed **randomly
  generated** numbers whenever the backend failed. All of this is replaced by real measurements.
- `InfluxDbService`: reusing a keep-alive HTTP connection to InfluxDB stalled ~40 ms per request (TCP delayed ACK
  through Docker's port forwarder), adding ~40 ms to every measured InfluxDB query. Idle connections are now closed,
  so each query opens a fresh one (~1 ms on localhost): a typical small Flux query went from ~52 ms to ~13 ms.
- `InfluxDbService`: a valid empty InfluxDB answer used to be replaced with 12 fake rows. Strict
  (no-fallback) methods were added for the comparison engine.
- 3D visualizer (`visual.html`) on **both** sites crashed on load. It imported the Firebase config that was
  removed from the package. The InfluxDB site was also missing the `three` dependency.
- 3D visualizer counted every device as OFFLINE (it checked `status == 'ON'`, but the schema uses `'ONLINE'`).
- InfluxDB site: `public/` folder (logo, home-card image, favicon) was missing, so landing-page images were broken.
- `Authentication.jsx` in all three sites was saved in a non-UTF-8 encoding, which showed garbled characters.
- Oracle sequences were not advanced past the sample-data IDs, so creating a home/room/device/reading failed with ORA-00001.
  Fixed by `09_sync_sequences.sql`, which `setup_all.sql` runs.
- `SensorReading` JSON serialisation of the lazy `device` proxy (`/api/readings/...`).
- ESLint config added to `frontend-influx` and `frontend-compare` (their `lint` scripts had none).
- **Comparison dashboard restyled** in the same light cream/brown design as the Oracle and InfluxDB sites
  (Newsreader serif headings, dark-brown stat cards, Smart Home logo, matching nav and footer).
- **InfluxDB analytics hub** (inside both smart-home sites, on a home's page):
  - the device list was hard-coded with wrong IDs and names; it now loads the 10 sensors that really have readings;
  - the chart's default `-30d` window excluded the sample readings (dated 17 Sep 2026), so the hub always fell back to
    invented points. It now reads all readings, and shows a clear message when InfluxDB has none;
  - "Start Live Query Race" now times the *same* hourly-average query on both databases (5 runs each) and says whether the
    results match. Before, it compared different queries, and it showed fixed numbers (8.10 ms / 142.40 ms / 17.6×) when it failed;
  - Section 6 "Run Live Checks" now really compares Oracle and InfluxDB (counts, averages, min/max, latest reading).
    Before, a timer simply counted up to "9 / 9 PASSING";
  - "Ingest Sample" re-syncs: it clears `sensor_reading` and copies all 120 Oracle readings, so there are never duplicates;
  - the on-screen "live stream" and "inject spike" are client-side demos and are now labelled *simulated* (stream off by default).
- `/api/influx/health` reported "healthy" even when InfluxDB was down; `/api/influx/query` and `/api/influx/write`
  returned fake rows and fake "buffered" writes on failure. All three now report the real error.
- New endpoints `GET /api/influx/devices` and `GET /api/influx/verify`.
- `nginx/nginx.conf` was missing, so `docker compose up` in the root (load-balancing demo) failed. It is now the round-robin config.
- The backend's `contextLoads` test no longer needs a running Oracle, so `mvnw package` works offline.
- All three websites now pass ESLint with zero errors.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Oracle pill red: `ORA-01017 invalid username/password` | Wrong `DB_PASSWORD`. Restart the backend with the right one. |
| Oracle pill red: `Connection refused` / `ORA-12541` | Start the `OracleServiceXE` and `OracleOraDB21Home1TNSListener` Windows services. |
| InfluxDB pill red | Start Docker Desktop, then `cd da3-influxdb-migration; docker compose up -d`. |
| InfluxDB `unauthorized` | The container was created earlier with another token. Run `docker compose down -v` in `da3-influxdb-migration`, then `up -d` again. **This deletes InfluxDB data.** |
| Dashboard says "Backend not reachable" | The backend window shows the reason. Usually Java is not version 21, or Oracle is down. |
| "Create Home" fails with ORA-00001 | Run `database/09_sync_sequences.sql` as SMART_HOME. |
| Port already in use | Close the old terminal window that is still running that site/backend. |
