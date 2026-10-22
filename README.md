# Smart Home Management System — Advanced DBMS Architecture

A full-stack, enterprise-grade Smart Home Management & Spatial Digital Twin platform built on **Spring Boot 4.1.1**, **Oracle Database 21c Express Edition**, **React 19 (Vite)**, **Three.js**, and **Firebase Authentication**.

The system features advanced relational schema design, dynamic spatial visualization, role-based security scoping, PL/SQL stored procedures, triggers, custom functions, and an in-browser interactive SQL/PL-SQL execution engine.

---

> 🤖 **Agent & Architecture Navigation:**
> - **Agent Operating Contract & Fast Path:** [`AGENTS.md`](AGENTS.md)
> - **Canonical System Architecture (Tower of Abstractions L0-L10):** [`docs/SYSTEM_ARCHITECTURE.md`](docs/SYSTEM_ARCHITECTURE.md)
> - **Standard Operating Runbooks:** [`docs/OPERATIONS.md`](docs/OPERATIONS.md)
> - **System State Model & Health:** [`docs/SYSTEM_STATE_MODEL.md`](docs/SYSTEM_STATE_MODEL.md)
> - **Docker Load Balancing Demo Script:** Run `.\scripts\demo.ps1`

---

## 🏗️ System Architecture

```text
+-----------------------------------------------------------------------------------+
|                           Client Presentation Layer                               |
|  React 19 SPA (Vite)  •  Tailwind CSS 4  •  Three.js Digital Twin Visualizer       |
+-----------------------------------------------------------------------------------+
                                     | HTTPS / Bearer Token
                                     v
+-----------------------------------------------------------------------------------+
|                            API & Business Layer                                   |
|                    Spring Boot 4.1.1 / Java 21 REST API                             |
|                                                                                   |
|  +---------------------------+  +-----------------------------------------------+  |
|  | Firebase Admin SDK        |  | Spring Security / JWT Filter                  |  |
|  | Token Verification        |  | Role & HomeAuthorizationService Scoping       |  |
|  +---------------------------+  +-----------------------------------------------+  |
|  +---------------------------+  +-----------------------------------------------+  |
|  | REST Controllers          |  | SqlConsoleService (DML Scoping Engine)        |  |
|  +---------------------------+  +-----------------------------------------------+  |
+-----------------------------------------------------------------------------------+
                                     | JDBC / HikariCP
                                     v
+-----------------------------------------------------------------------------------+
|                            Data Persistence Layer                                 |
|                   Oracle Database 21c Express Edition                             |
|                                                                                   |
|  • 17 Relational Tables (Normalized EER Master Schema)                            |
|  • Sequences (SEQ_USER_ID, SEQ_HOME_ID, SEQ_DEVICE_ID, etc.)                     |
|  • PL/SQL Stored Procedures & Functions                                           |
|  • Triggers (TRG_PREVENT_DEVICE_CYCLE)                                            |
+-----------------------------------------------------------------------------------+
```

### Authentication & Security Architecture:
```
1. Client (Google Sign-In / Email Password) ---> Firebase Auth
2. Firebase Auth ---> Returns Firebase ID Token (JWT)
3. Client ---> Sends Bearer Token to Spring Boot (`/api/auth/me`)
4. FirebaseAuthenticationFilter ---> Verifies Token with Firebase Admin SDK
5. UserProvisioningService ---> Resolves / Provisions User in Oracle USERS Table
6. Spring Security ---> Sets SecurityContext with Verified Role (ADMIN / USER)
```

---

## 📂 Repository Structure

```text
dbms/
├── backend/                      # Spring Boot 4.1.1 / Java 21 REST API
│   ├── src/main/java/            # Controllers, Services, Repositories, Entities, Security
│   ├── src/main/resources/       # application.properties
│   ├── src/test/java/            # JUnit integration tests & security verification
│   ├── .env.example              # Backend environment template
│   ├── pom.xml                   # Maven dependencies
│   └── mvnw / mvnw.cmd           # Maven wrapper
│
├── frontend/                     # React 19 / Vite Single Page Application
│   ├── src/pages/                # Landing, Authentication, HomeDetails, DeviceCategoryView
│   ├── src/components/           # SqlConsole, DeviceCard, Navbar, Modals
│   ├── src/config/               # Firebase Web Client configuration
│   ├── src/context/              # AuthContext (React Context)
│   ├── src/services/             # Axios API client & centralized API base URL config
│   ├── visual.html               # Three.js 3D Digital Twin Console
│   ├── .env.example              # Frontend environment template
│   ├── package.json              # Node dependencies
│   └── vite.config.js            # Vite build configuration
│
├── database/                     # Oracle 21c SQL & PL/SQL Master Scripts
│   ├── 01_create_tables.sql      # Creates all 17 tables
│   ├── 02_constraints.sql        # Primary keys, foreign keys, checks
│   ├── 03_sequences.sql          # Surrogate sequence generators
│   ├── 04_sample_data.sql        # Core baseline dataset
│   ├── 04b_expand_sample_data.sql# Expanded realistic dataset
│   ├── 05_queries.sql            # Analytics & verification queries
│   ├── 06_plsql.sql              # Stored procedures, functions, triggers
│   └── 07_auth_extension.sql     # Firebase UID mapping & role extension
│
└── docs/                         # System & Deployment Documentation
    ├── postman/                  # Postman collection & test verification suite
    └── deployment/               # Phase 10 deployment documentation report
```

---

## 💻 Local Development Setup

The local development setup remains fully supported and is the primary environment for feature development.

### Prerequisites:
1. **Java 21 JDK** installed and configured in `PATH`.
2. **Node.js 18+** and `npm`.
3. **Oracle Database 21c Express Edition (XE)** running locally on port `1521` with PDB `XEPDB1`.
4. **Firebase Project** with Authentication enabled (Google & Email/Password providers).
5. **Firebase Admin SDK Service Account Key JSON file** located on developer machine.

### Step 1: Database Setup
Connect to local Oracle XE as `SYSDBA` or a dedicated schema user (`SMART_HOME`) and execute the SQL scripts in order:
```sql
@database/01_create_tables.sql
@database/02_constraints.sql
@database/03_sequences.sql
@database/04_sample_data.sql
@database/04b_expand_sample_data.sql
@database/06_plsql.sql
@database/07_auth_extension.sql
```

### Step 2: Backend Configuration & Launch
Start the Spring Boot backend on port `8080`:
```powershell
cd backend
$env:DB_PASSWORD="YourOraclePassword"
$env:ADMIN_GOOGLE_EMAIL="admin@example.com"
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path	o\service-account.json"
.\mvnw.cmd spring-boot:run
```

The API will be accessible locally at `http://localhost:8080`.

### Step 3: Frontend Configuration & Launch
Start the React/Vite development server on port `5173`:
```powershell
cd frontend
npm install
npm run dev
```

The application UI will be accessible locally at `http://localhost:5173`.

---

## 🌐 Project Deployment & Topology Overview

### Usage Matrix

| Mode | Frontend Host | Backend URL / Tunnel | Database Target | Purpose / Status |
| :--- | :--- | :--- | :--- | :--- |
| **LOCAL** | `localhost:5173` | `http://localhost:8080` | Oracle XE (`localhost:1521/XEPDB1`) | **Primary Dev Setup** — Fully supported & verified |
| **TEMPORARY DEMO** | Vercel CDN | Cloudflare Quick Tunnel (`*.trycloudflare.com`) | Oracle XE (Developer PC) | **Experimental Public Access** — Backend verified via API, browser end-to-end unverified |
| **FUTURE PERMANENT** | Vercel CDN | Persistent Named Tunnel / Hosted Cloud Service | Production Oracle DB | **Recommended Target** — Planned production architecture |

---

## 🚀 Temporary Quick Tunnel Setup

For temporary public demonstration or remote frontend testing, a Cloudflare Quick Tunnel can be used to expose the locally running Spring Boot backend.

```text
+-----------------------------------------------------------------------------------+
|                                React/Vite Frontend                                |
|                              (Deployed on Vercel)                                 |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                              Cloudflare Quick Tunnel                              |
|                   (e.g., https://<random-id>.trycloudflare.com)                   |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                Spring Boot Backend                                |
|                             (Running on localhost:8080)                           |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                              Oracle Database 21c XE                               |
|                            (Running on localhost:1521)                            |
+-----------------------------------------------------------------------------------+
```

### Exposing Local Backend via Cloudflare Quick Tunnel:

1. Ensure local Spring Boot backend and Oracle XE are running on port `8080` and `1521`.
2. Run the exact Cloudflare command:
   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```
3. Cloudflare will output a temporary URL formatted like:
   `https://dover-hitachi-base-pubmed.trycloudflare.com`

> [!WARNING]
> **IMPORTANT NOTICE REGARDING QUICK TUNNEL URLS:**
> - Cloudflare Quick Tunnels generate **temporary** `trycloudflare.com` URLs that change every time the command or process is restarted.
> - The `cloudflared` process must remain running continuously for the tunnel to remain available.
> - If the tunnel URL changes, any frontend deployment using that URL as `VITE_API_URL` must be updated with the new value and rebuilt.
> - The local Spring Boot service and local Oracle XE database machine must remain online and active.
> - **DO NOT** hardcode temporary `trycloudflare.com` URLs into application source code.
> - This setup is strictly intended for temporary public demonstration, developer experimentation, or remote API testing — **NOT** as a permanent production architecture.

---

## 📊 Current Deployment Status

The current status of components across local, public, and cloud layers is detailed below:

| Component | Status | Verification Detail |
| :--- | :--- | :--- |
| **GitHub Repository Setup** | **Completed** | Clean repository layout containing `backend/`, `frontend/`, `database/`, `docs/`, `.gitignore`, and `README.md`. |
| **Local Backend & Database** | **Verified** | Spring Boot 4.1.1 runs on port `8080` connecting to Oracle XE 21c (`XEPDB1`). |
| **Public Backend Tunnel** | **Verified** | Cloudflare Quick Tunnel endpoint successfully passed authenticated Postman/Newman testing: <br>• `ADMIN GET /api/homes` -> `200 OK` (all 5 homes)<br>• `USER GET /api/homes` -> `200 OK` (only authorized Home 1)<br>• `USER GET /api/homes/2` -> `403 Forbidden` (security scoping enforced) |
| **Frontend Production Build** | **Verified** | Centralized API configuration using `VITE_API_URL`. Production build via `npm run build` succeeds without errors. |
| **Vercel Deployment** | **Created (Ready)** | Frontend static deployment successfully created and reached Ready status on Vercel platform. |
| **Final End-to-End Browser Integration** | **Unverified** | **Note:** The complete end-to-end browser flow (Vercel CDN -> Cloudflare Quick Tunnel -> Local Spring Boot -> Oracle XE) has **not** been fully verified in a production browser environment. |

> [!IMPORTANT]
> **Summary Statement:** The deployment is currently a **temporary public demonstration setup** with **verified backend tunnel connectivity** and a **Vercel deployment created**, but **final browser end-to-end integration remains unverified**.

---

## 🔮 Future Permanent Deployment Path

To transition the project from a temporary demonstration setup into a stable production deployment, the following standard production options are recommended:

```text
                                  [ Browser ]
                                       |
                                       v
                                [ Vercel CDN ]
                                       |
                                       v
                        [ Stable Public Backend Host ]
                                       |
                                       v
                          [ Spring Boot Backend API ]
                                       |
                                       v
                          [ Production Oracle Database ]
```

### Option A — Persistent Named Cloudflare Tunnel:
- Configure a persistent, named Cloudflare Tunnel (using a Cloudflare account) associated with a stable custom domain/hostname (e.g., `https://api.yourdomain.com`).
- Set `VITE_API_URL` on Vercel to point to this stable hostname.
- This eliminates backend URL changes when restarting the tunnel process.
- *Requirement:* The local developer machine running Spring Boot and Oracle XE must still remain powered on and connected to the internet.

### Option B — Fully Hosted Cloud Infrastructure (Recommended Production Architecture):
- Deploy the Spring Boot containerized application to a cloud host such as **Railway**, **Render**, or **AWS ECS/App Runner**.
- Connect the cloud backend service to a cloud-hosted relational database.
- Update the Vercel frontend environment variable `VITE_API_URL` to point to the cloud backend's permanent URL.

### Note on Database Infrastructure Choice:
The development environment relies on **Oracle XE 21c** (`localhost:1521/XEPDB1`). The project was intentionally retained on local Oracle XE rather than migrating to Oracle Autonomous AI Database on OCI because the developer was unable to complete the Oracle Cloud sign-up process requiring credit card verification. Production cloud database migration remains a future step.

---

## 🔒 Security Notes & Hardening

1. **Database Passwords & Credentials:** Passwords must never be hardcoded or committed to version control. Always pass via environment variables (e.g. `DB_PASSWORD`).
2. **Firebase Admin Service Account:** The service account JSON key file contains sensitive private keys and must be stored outside the Git repository tree. Reference it using `GOOGLE_APPLICATION_CREDENTIALS`.
3. **Firebase ID Tokens:** User Firebase JWT tokens expire after 1 hour and must never be committed or logged.
4. **Client-Side Environment Variables:** All `VITE_*` variables defined in `frontend/.env` are exposed in public client JavaScript bundles. Never include backend secrets, database connection strings, or service account keys in `VITE_*` variables.
5. **Database Network Exposure:** Oracle XE port `1521` should **never** be exposed directly to the public internet. HTTP application traffic is proxied safely through Spring Boot REST endpoints and Cloudflare Tunnel encapsulation.

---

## ⚙️ PL/SQL Procedures, Functions & Triggers

The system leverages compiled Oracle PL/SQL objects:

1. **`FN_CALCULATE_DEVICE_UPTIME(p_device_id)` (Function):**
   Computes derived uptime intervals (`X days, Y hours, Z minutes`) by comparing `SYSTIMESTAMP` against `last_restart_time` or `install_date`.
2. **`FN_CHECK_DEVICE_ACCESS(p_user_id, p_device_id)` (Function):**
   Evaluates M:N authorization across `HOME_ACCESS`, `ROOMS`, and `DEVICES`, returning `1` (authorized) or `0` (denied).
3. **`SP_ACKNOWLEDGE_ALERT(p_alert_id, p_user_id)` (Procedure):**
   Locks alert row via `FOR UPDATE NOWAIT`, verifies pending state, and updates status to `ACKNOWLEDGED` with audit timestamps.
4. **`SP_HOME_DEVICE_HEALTH_REPORT(p_home_id)` (Procedure):**
   Uses an **explicit cursor** (`CURSOR c_devices IS ...`) to iterate across all room devices and streams formatted diagnostic lines via `DBMS_OUTPUT`.
5. **`SP_RECORD_SENSOR_READING(p_device_id, p_value, p_reading_id OUT)` (Procedure):**
   Fetches surrogate `reading_id` from `SEQ_READING_ID.NEXTVAL` and inserts weak entity metric.
6. **`TRG_PREVENT_DEVICE_CYCLE` (Trigger):**
   `BEFORE INSERT OR UPDATE OF parent_device_id ON DEVICES` trigger blocking circular self-parent hierarchy assignments (`ORA-20030`).

---

## 🎨 3D Digital Twin Visualizer

Access the visualizer at:  
`http://localhost:5173/visual.html?homeId=1`

### Capabilities:
- **Dynamic Estate Generation:** Synthesizes parametric architectural floor plans, exterior shells, double-loaded corridors, central stairwell shafts, and streetscapes directly from live backend API data.
- **Device Subtype 3D Geometries:**
  - **Smart Light:** Lamp base, stem pole, tapered shade, glass bulb, and attached `THREE.PointLight`.
  - **Temperature Sensor:** Base plate, mast, 3 horizontal tiered discs, top status LED beacon.
  - **Thermostat:** Wall plate, LCD digital display, status LED.
  - **Motion Sensor:** Bracket plate, angled hemispherical sensor dome (90° - 0.3 rad).
  - **Camera:** Articulated housing, cylindrical lens bezel, -0.42 rad downward pitch gaze.
- **Live Status Visuals:** Dynamic emissive glow indicating `ONLINE` (active color), `OFFLINE` (dim gray), or `ERROR` (amber/red).
- **In-Memory Command Console:** Type natural English commands (e.g. `add room Gym on floor 1`) for client-side spatial prototyping without altering the Oracle database.

---

## 📡 REST API Reference

| Category | Method & Path | Access | Description |
| :--- | :--- | :--- | :--- |
| **Auth** | `GET /api/auth/me` | Authenticated | Resolves Firebase token to Oracle User identity & role |
| **Homes** | `GET /api/homes` | Scoped / Admin | Lists accessible residences |
| | `GET /api/homes/{id}` | Scoped / Admin | Fetches single home details |
| | `GET /api/homes/{id}/health-report`| Scoped / Admin | Executes PL/SQL health report procedure |
| | `POST /api/homes` | Authenticated | Creates residence & assigns `OWNER` role |
| | `PUT /api/homes/{id}` | Owner / Admin | Updates residence address & details |
| | `DELETE /api/homes/{id}` | Owner / Admin | Deletes residence and cascades child spaces |
| **Rooms** | `GET /api/rooms/home/{homeId}` | Scoped / Admin | Lists rooms in residence |
| | `POST /api/rooms/home/{homeId}` | Scoped / Admin | Creates room on designated floor |
| | `DELETE /api/rooms/{id}` | Scoped / Admin | Deletes room |
| **Devices** | `GET /api/devices/home/{homeId}/detailed` | Scoped / Admin | Fetches devices with subtype specializations |
| | `GET /api/devices/{id}/uptime` | Scoped / Admin | Computes uptime via `FN_CALCULATE_DEVICE_UPTIME` |
| | `PATCH /api/devices/{id}/status` | Scoped / Admin | Toggles device status (`ONLINE` / `OFFLINE`) |
| **Alerts** | `GET /api/alerts` | Scoped / Admin | Lists active/acknowledged alerts |
| | `POST /api/alerts/{id}/acknowledge` | Scoped / Admin | Acknowledges alert via `SP_ACKNOWLEDGE_ALERT` |
| **Readings**| `GET /api/readings/device/{deviceId}` | Scoped / Admin | Lists telemetry history |
| | `POST /api/readings/device/{deviceId}` | Scoped / Admin | Records reading via `SP_RECORD_SENSOR_READING` |
| **SQL Console**| `POST /api/sql-console/execute` | Scoped / Admin | Executes validated SQL queries & DML statements |
| | `POST /api/sql-console/execute-plsql`| Scoped / Admin | Executes allowlisted PL/SQL stored routines |

---

## 🧪 Postman API Verification Suite

The repository contains an automated Postman collection verified with Newman:
- **Collection:** `docs/postman/SmartHome-API.postman_collection.json`
- **Environment:** `docs/postman/SmartHome-Local.postman_environment.json`
- **Coverage:** 62+ requests, 127+ automated assertions across all controllers, roles, and error states.

### Running with Newman:
```bash
npm install -g newman
newman run docs/postman/SmartHome-API.postman_collection.json \
  -e docs/postman/SmartHome-Local.postman_environment.json \
  --env-var "adminToken=<YOUR_FIREBASE_ADMIN_TOKEN>" \
  --env-var "userToken=<YOUR_FIREBASE_USER_TOKEN>"
```

---

## 📊 Database Baseline

The development baseline contains **376 relational rows** across 17 master tables:

| Table | Rows | Table | Rows |
| :--- | :--- | :--- | :--- |
| `USERS` | 10 | `AUTOMATION_RULES` | 15 |
| `HOMES` | 5 | `RULE_ACTIONS` | 25 |
| `ROOMS` | 23 | `ALERT_CATEGORIES` | 5 |
| `DEVICES` | 45 | `ALERTS` | 35 |
| `SMART_LIGHTS` | 10 | `HOME_ACCESS` | 14 |
| `THERMOSTATS` | 5 | `USER_CONTACT_NUMBERS` | 14 |
| `TEMPERATURE_SENSORS`| 10 | `NOTIFICATION_PREFERENCES` | 25 |
| `MOTION_SENSORS` | 8 | `SENSOR_READINGS` | 120 |
| `CAMERAS` | 7 | **TOTAL** | **376** |

---

## 🛠️ Troubleshooting

- **Oracle Connection Refused (`ORA-12541` / `Connection refused`):** Verify Oracle XE service is running (`Get-Service OracleServiceXE`) and listening on port `1521`.
- **Firebase Token Verification Failed (`401 Unauthorized`):** Verify `GOOGLE_APPLICATION_CREDENTIALS` points to a valid service account JSON key and the client token is not expired (Firebase tokens expire after 1 hour).
- **CORS Errors in Browser Console:** Verify `CORS_ALLOWED_ORIGINS` in `backend/src/main/resources/application.properties` includes the frontend origin (`http://localhost:5173` or Vercel origin).
- **Circular Identifier in application.properties:** Ensure environment variable keys do not map identically to placeholder names without prefixes.

---

## 📄 License

All rights reserved. Developed as part of the Advanced DBMS Smart Home Engineering Curriculum.
