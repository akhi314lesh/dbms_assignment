# Smart Home IoT Monitoring System — DA3 Team Sharing Package

Welcome to the **DA3 Smart Home IoT Monitoring System** handover package!

This package contains the complete, completed frontend and backend code for DA3, including the three separated web applications, the InfluxDB 2.x migration engine, sample telemetry datasets, and the side-by-side Oracle vs InfluxDB comparison dashboard.

---

## 🏗️ System Architecture & Website Topology

The project is decoupled into three independent websites and a dual-database Spring Boot backend:

| Port | Service / Web Application | Directory | Purpose |
| :--- | :--- | :--- | :--- |
| **`5173`** | **Oracle Smart Home Dashboard** | `frontend/` | Complete luxury Smart Home UI backed by Oracle 21c XE relational tables, SQL Console, and 3D Digital Twin Visualizer. |
| **`5174`** | **InfluxDB Smart Home Dashboard** | `frontend-influx/` | Full Smart Home UI internally running on InfluxDB 2.x time-series engine, live telemetry gauges, 3D Visualizer, and Flux Console. |
| **`5175`** | **Comparison & Benchmark Dashboard** | `frontend-compare/` | Empirical performance race, query latency benchmarks, speedup factor badge, run history, and technical capability matrix. |
| **`8080`** | **Spring Boot Dual-DB Backend** | `backend/` | Spring Boot 3.x / Java 21 REST API connecting to Oracle 21c XE JDBC and InfluxDB 2.x Flux SDK (`/api/compare/**`, `/api/influx/**`). |
| **`8086`** | **InfluxDB 2.x TSDB Engine** | `da3-influxdb-migration/` | Time-series storage with `sensor_telemetry` bucket holding 20,000+ points. |

---

## 🚀 Quick Start Guide

### 1. Prerequisites

- **Java Development Kit (JDK) 21** (`java -version`)
- **Node.js 18+ and npm** (`node -v`, `npm -v`)
- **Docker Desktop** (for InfluxDB 2.x and optionally Nginx / containerized backends)
- **Oracle Database 21c XE** running locally on port `1521` with pluggable database `XEPDB1` and user `SMART_HOME`.
- **Python 3.10+** (optional, for running the standalone telemetry generation daemon in `da3-influxdb-migration/`).

---

### 2. Configuration & Environment Variables

Copy the provided `.env.example` templates to `.env`:

```powershell
# In project root
Copy-Item .env.example .env

# In backend directory
Copy-Item backend\.env.example backend\.env
```

Review and adjust `.env` parameters if needed:
- `DB_PASSWORD`: Password for your local Oracle Database `SMART_HOME` schema.
- `SPRING_DATASOURCE_URL`: `jdbc:oracle:thin:@localhost:1521/XEPDB1`
- `INFLUXDB_URL`: `http://localhost:8086`
- `INFLUXDB_TOKEN`: `da3_super_secret_sensor_telemetry_token_123456789`
- `INFLUXDB_ORG`: `smarthome_org`
- `INFLUXDB_BUCKET`: `sensor_telemetry`

---

### 3. Start InfluxDB 2.x

Start InfluxDB using the Docker Compose file inside `da3-influxdb-migration/`:

```powershell
cd da3-influxdb-migration
docker compose up -d
```

Verify InfluxDB is listening:
```powershell
curl.exe http://localhost:8086/health
```

---

### 4. Install Frontend Dependencies

Install dependencies in all three frontend directories:

```powershell
# 1. Oracle Frontend
cd frontend
npm install

# 2. InfluxDB Frontend
cd ..\frontend-influx
npm install

# 3. Comparison Dashboard Frontend
cd ..\frontend-compare
npm install
```

---

### 5. Launch the Application

You can launch all services at once using the automated PowerShell runner:

```powershell
.\scripts\start-local.ps1
```

Or start each component individually in separate terminal windows:

#### Terminal 1 — Spring Boot Backend (:8080)
```powershell
cd backend
$env:DB_PASSWORD="your_oracle_password"
.\mvnw.cmd spring-boot:run
```

#### Terminal 2 — Oracle Smart Home Dashboard (:5173)
```powershell
cd frontend
npm run dev
```

#### Terminal 3 — InfluxDB Smart Home Dashboard (:5174)
```powershell
cd frontend-influx
npm run dev
```

#### Terminal 4 — Comparison Benchmark Dashboard (:5175)
```powershell
cd frontend-compare
npm run dev
```

---

## 🛠️ Comparison Dashboard (T1 + T3): done

The comparison backend (`/api/compare/**`) and the dashboard on port `5175` are complete and connected.
**See [`RUN_GUIDE.md`](RUN_GUIDE.md)** for setup, the API reference, how fairness is ensured, and the list of fixes.

Key files:
- `backend/src/main/java/com/smarthome/smart_home_backend/service/CompareService.java` — comparison engine
- `backend/src/main/java/com/smarthome/smart_home_backend/service/FluxCsvParser.java` — InfluxDB CSV parser
- `backend/src/main/java/com/smarthome/smart_home_backend/controller/CompareController.java` — REST API
- `frontend-compare/src/components/ComparisonDashboard.jsx` + `charts.jsx` — dashboard UI

---

## 🔒 Authentication Note for Development

To make local development and testing seamless across all machines without external cloud dependencies:
- **Firebase integration has been decoupled**.
- Spring Security is configured in **local development mode** (`permitAll()`).
- All frontends run with an automatic built-in developer identity (`DA3 Teammate`, role `ADMIN`).
- You can immediately explore all residences, devices, 3D visualizers, and comparison benchmarks without any login setup or token errors.
