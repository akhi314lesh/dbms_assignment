# DA3: Isolated InfluxDB Sensor Data Migration & Validation

A fully self-contained, portable, time-series data migration and analytical query component built for **DBMS Coursework DA3**.

This component demonstrates how time-series IoT sensor data can be ingested, managed, and queried using **InfluxDB 2.x** and **Flux**, operating as an alternative database paradigm to relational and document stores.

---

## ⚡ 1-Minute Quickstart (If Prerequisites are Installed)

Open PowerShell inside the `da3-influxdb-migration` directory and run:

```powershell
# Run the complete automated demo (checks prereqs, starts InfluxDB, migrates data, queries, and tests):
.\run_demo.ps1
```

To stop the isolated database container when done:
```powershell
.\stop_influxdb.ps1
```

---

## 💻 Fresh Windows Laptop Setup Guide (From Scratch)

If you are handing this project to someone who has a completely fresh Windows laptop with **nothing installed**, they only need two free tools: **Python 3** and **Docker Desktop**.

### Step 1: Install Python 3
- **Option A (Fastest via Windows Terminal / PowerShell)**:
  ```powershell
  winget install Python.Python.3.11
  ```
- **Option B (Manual Installer)**:
  1. Download Python 3 from [python.org/downloads](https://www.python.org/downloads/).
  2. **CRITICAL:** Check the box **"Add python.exe to PATH"** at the bottom of the installer wizard before clicking Install.
  3. Verify in a new PowerShell window:
     ```powershell
     python --version
     ```

### Step 2: Install Docker Desktop
- **Option A (Fastest via PowerShell)**:
  ```powershell
  winget install Docker.DockerDesktop
  ```
- **Option B (Manual Installer)**:
  1. Download from [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/).
  2. Run the installer and accept the WSL 2 backend recommendation.
  3. Restart your laptop if prompted by Windows.
  4. Launch **Docker Desktop** from the Start Menu and wait until the whale icon in the taskbar turns solid green (Docker Engine running).

### Step 3: Run the Prerequisite Checker
Open PowerShell, navigate to this folder, and run:
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\check_prerequisites.ps1
```
If everything is green `[PASS]`, the laptop is ready.

---

## 📦 What is Inside This Component?

Everything is strictly self-contained within `da3-influxdb-migration/` with zero external project dependencies:

```text
da3-influxdb-migration/
├── docker-compose.yml          # Isolated InfluxDB 2.7.11 Alpine container on port 8086
├── .env.example                # Safe environment template with default credentials
├── .gitignore                  # Protects local .env, caches, and test artifacts
├── requirements.txt            # Optional pip dependencies (influxdb-client, pytest)
├── README.md                   # This portable setup & handover documentation
│
├── data/
│   └── sample_sensor_readings.csv   # 60 Oracle-derived sensor telemetry records
│
├── scripts/
│   ├── migrate_readings.py     # Parses CSV, validates, deduplicates, writes to InfluxDB
│   └── query_readings.py       # Executes 3 analytical Flux queries (latest, avg, count)
│
├── tests/
│   └── test_sensor_migration.py # 9 unit & integration tests with mathematical ground truths
│
├── check_prerequisites.ps1     # Validates Python, Docker, and port availability
├── start_influxdb.ps1          # Starts container & health-polls until HTTP 200 ready
├── run_demo.ps1                # All-in-one 1-click execution & verification script
└── stop_influxdb.ps1           # Stops container cleanly without data loss
```

---

## 🛡️ Portability & Zero-Dependency Design

To guarantee that this component runs on **any laptop** even without an active internet connection or working `pip`:

1. **Standard Library HTTP Fallback**:
   - `scripts/migrate_readings.py` and `scripts/query_readings.py` prioritize the official `influxdb-client` if installed.
   - If `influxdb-client` is not installed or `pip` is unavailable, both scripts automatically fall back to Python's built-in standard library (`urllib.request`).
   - Line Protocol write payload and Flux CSV query parsing work out-of-the-box with **zero third-party pip packages required**.

2. **Standalone Sample Dataset**:
   - The recipient does **not** need an Oracle database, SQL*Plus, or database drivers.
   - The `data/sample_sensor_readings.csv` dataset contains 60 verified records directly mirroring the project's Oracle schema (`device_id`, `reading_id`, `value`, `reading_time`, `device_name`, `device_subtype`, `unit`).

3. **Isolated Docker Footprint**:
   - Pinned image: `influxdb:2.7.11-alpine` (lightweight, under 150MB).
   - Container name: `da3-influxdb-server` (does not collide with any existing containers).
   - Storage volume: `da3-influxdb-storage`.
   - Host port: `8086` (configurable via `INFLUXDB_PORT` in `.env`).

---

## 🚀 Step-by-Step Manual Execution

If you prefer running commands manually rather than using `run_demo.ps1`:

### 1. Start InfluxDB
```powershell
.\start_influxdb.ps1
```
*Wait for output: `[+] InfluxDB 2.7.11 is UP and HEALTHY on port 8086!`*

### 2. Run Migration (Data Ingestion)
```powershell
# Optional Dry-run (validates 60 records without writing):
python scripts/migrate_readings.py --dry-run

# Live write to InfluxDB:
python scripts/migrate_readings.py
```

**Expected Migration Summary:**
```text
==================================================
        INFLUXDB SENSOR MIGRATION SUMMARY        
==================================================
  Total records read:         60
  Successfully transformed:   60
  Successfully written:       60
  Skipped duplicates:         0
  Rejected records:           0
==================================================
```

### 3. Run Analytical Flux Queries
```powershell
# Query 1: Latest reading for Device 4 (Kitchen Temp Sensor)
python scripts/query_readings.py --query latest --device 4

# Query 2: Average temperature for Device 4
python scripts/query_readings.py --query avg --device 4 --start 2026-09-17T00:00:00Z --stop 2026-09-18T00:00:00Z

# Query 3: Reading count for Device 4
python scripts/query_readings.py --query count --device 4 --start 2026-09-17T00:00:00Z --stop 2026-09-18T00:00:00Z

# Run all 3 queries at once:
python scripts/query_readings.py --query all --device 4
```

**Expected Query Outputs:**
- **Query 1 (Latest Reading):** `23.50 °C` at `2026-09-17 19:00:00 UTC`
- **Query 2 (Average Temperature):** `24.66 °C` (Ground truth: 295.90 / 12 readings)
- **Query 3 (Count):** `12 readings` for Device 4 (`60 readings` across the estate)

### 4. Run Automated Validation Test Suite
```powershell
python -m unittest tests/test_sensor_migration.py -v
```

**Expected Test Output:**
```text
test_csv_file_exists_and_loads ... ok
test_deterministic_device_4_math ... ok
test_flux_query_builders ... ok
test_idempotency_deduplication_logic ... ok
test_live_migration_and_query_flow ... ok
test_record_transformation_valid ... ok
test_rejection_invalid_timestamp ... ok
test_rejection_invalid_value ... ok
test_rejection_missing_device_id ... ok

----------------------------------------------------------------------
Ran 9 tests in 3.421s
OK
```

### 5. Stop InfluxDB
```powershell
.\stop_influxdb.ps1
```

---

## 🔧 Troubleshooting on New Laptops

| Issue | Cause | Immediate Resolution |
| :--- | :--- | :--- |
| **`scripts.ps1 cannot be loaded because running scripts is disabled`** | Windows PowerShell execution policy restriction. | Run: <br>`Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` |
| **`docker info` fails / connection refused** | Docker Desktop application is closed or initializing. | Open **Docker Desktop** from the Windows Start menu and wait 15–30 seconds for the engine to initialize. |
| **Port 8086 already in use** | Another service on the recipient laptop uses port 8086. | Edit `.env` and set `INFLUXDB_PORT=8087`, then edit `INFLUXDB_URL=http://localhost:8087`. |
| **`python` opens Windows Store instead of running** | Windows App Execution Alias redirect. | In Windows Settings, search "Manage App Execution Aliases" and toggle OFF "App Installer (python.exe)". |

---

## 🤝 Project Handover Instructions

To hand this component over to another student or instructor:

1. Copy the entire `da3-influxdb-migration/` folder onto a USB drive or compress it as `da3-influxdb-migration.zip`.
2. Do **not** include `.env` (it will be auto-generated from `.env.example`).
3. The recipient unzips the folder, opens PowerShell, and simply executes:
   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   .\run_demo.ps1
   ```
4. All work is self-verifying, independent, and safe.
