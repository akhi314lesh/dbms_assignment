# System Capability Catalog — Machine-Legible Model

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Formal input/output/precondition/rollback cards for all major capabilities  

---

## 1. Capability: Core Device & Estate Telemetry API

- **Capability ID:** `CAP-001`
- **Purpose:** Provide authenticated REST CRUD operations for Homes, Rooms, IoT Devices, Subtype Specializations, and Sensor Telemetry.
- **Inputs:** HTTP GET/POST/PUT/DELETE requests with `Authorization: Bearer <Firebase_JWT>` header.
- **Outputs:** JSON responses conforming to DTO schemas; HTTP status codes (200, 201, 400, 401, 403, 404).
- **Dependencies:** `host-oracle-xe` (:1521), `firebase-admin-sdk`, Spring Boot REST controllers.
- **Preconditions:** Oracle XE running, schema populated (376 baseline rows), valid Firebase ID token provided.
- **Side Effects:** Inserts/updates relational rows in Oracle DB (`SENSOR_READINGS`, `DEVICES`, `ALERTS`).
- **Failure Modes:**
  - `401 Unauthorized`: Token missing, expired, or invalid Firebase project.
  - `403 Forbidden`: Authenticated user does not possess `OWNER` or authorized role in `HOME_ACCESS`.
  - `500 Internal Error`: Database connection pool exhausted or constraint violation.
- **Verification:**
  ```powershell
  curl.exe -s -o /dev/null -w "%{http_code}" http://localhost:8000/api/homes
  # Output must be 401 (proves Spring Security actively guards endpoint)
  ```
- **Rollback:** Transaction rollback managed automatically by Spring Data JPA `@Transactional` boundaries.
- **Resource Cost:** Low (5–15ms per request).
- **Security Sensitivity:** High (guarded by `HomeAuthorizationService`).

---

## 2. Capability: Nginx Round-Robin Load Balancing

- **Capability ID:** `CAP-002`
- **Purpose:** Distribute incoming HTTP traffic evenly across `backend1` and `backend2` using standard round-robin.
- **Inputs:** Inbound HTTP requests on `http://localhost:8000/*`.
- **Outputs:** HTTP responses proxied from upstream servers with upstream address logged in `/var/log/nginx/access.log`.
- **Dependencies:** `smart-home-nginx`, `smart-home-backend-1:8080`, `smart-home-backend-2:8080`.
- **Preconditions:** Both backend containers operational and registered in `upstream backend_cluster`.
- **Side Effects:** Zero persistence; stateless proxying.
- **Failure Modes:**
  - `502 Bad Gateway`: One or both backends crashed or unreachable on `:8080`.
- **Verification:**
  ```powershell
  .\scripts	est-round-robin.ps1
  # Output must show alternating sequence: backend-1 -> backend-2 -> backend-1...
  ```
- **Rollback:** Re-apply `nginx/nginx-round-robin.conf` and execute `docker exec smart-home-nginx nginx -s reload`.
- **Resource Cost:** Extremely Low (<1ms latency overhead).
- **Security Sensitivity:** Low.

---

## 3. Capability: Dynamic Weighted Traffic Rebalancing

- **Capability ID:** `CAP-003`
- **Purpose:** Dynamically re-weight upstream servers (3:1 ratio) to divert 75% traffic to `backend1` and 25% to `backend2`.
- **Inputs:** `nginx/nginx-weighted.conf` reloaded via Nginx signal `HUP` (`nginx -s reload`).
- **Outputs:** In-flight connection preservation, updated upstream distribution.
- **Dependencies:** `smart-home-nginx`, active configuration files.
- **Preconditions:** `nginx/nginx-weighted.conf` exists with syntax verified.
- **Side Effects:** Overrides active `nginx/nginx.conf` file.
- **Failure Modes:**
  - Nginx syntax error on reload (rejected by master process, old config retained).
- **Verification:**
  ```powershell
  .\scripts	est-weighted.ps1
  # Output: 40 requests yield ~30 (75%) to backend-1 and ~10 (25%) to backend-2
  ```
- **Rollback:**
  ```powershell
  Copy-Item nginx/nginx-round-robin.conf nginx/nginx.conf -Force
  docker exec smart-home-nginx nginx -s reload
  ```
- **Resource Cost:** Low (0 downtime during reload).
- **Security Sensitivity:** Low.

---

## 4. Capability: Spatial Digital Twin & 3D Visualization

- **Capability ID:** `CAP-004`
- **Purpose:** Synthesize interactive 3D architectural floorplans, rooms, and devices in real-time Three.js canvas.
- **Inputs:** Browser loading `visual.html?homeId=1`.
- **Outputs:** WebGL 3D rendered scene with dynamic device status glows (`ONLINE`, `OFFLINE`, `ERROR`).
- **Dependencies:** React frontend / Vite static server, backend `/api/homes/{id}`, `/api/rooms/home/{id}`, `/api/devices/home/{id}/detailed`.
- **Preconditions:** Backend operational; home `id=1` exists in Oracle database.
- **Side Effects:** Read-only queries to backend.
- **Failure Modes:** WebGL context failure, 401 unauthorized on missing token.
- **Verification:** Open `http://localhost:5173/visual.html?homeId=1` in Chrome.
- **Resource Cost:** Client-side GPU rendering.
- **Security Sensitivity:** Medium (scanned for home access permissions).

---

## 5. Capability: Automated Diagnostic & Demonstration Dashboard

- **Capability ID:** `CAP-005`
- **Purpose:** Execute comprehensive 7-point verification suite in terminal for examiners, video capture, and CI.
- **Inputs:** Execution of `.\scripts\demo.ps1`.
- **Outputs:** Color-coded formatted ASCII dashboard covering Docker, containers, round-robin, weighted test, logs, stats.
- **Dependencies:** PowerShell 5.1+, Docker CLI, active containers.
- **Preconditions:** Containers running via `docker compose up -d`.
- **Side Effects:** Emits 10 test requests in round-robin mode and 40 test requests in weighted mode; restores round-robin.
- **Failure Modes:** Non-zero exit code if any single assertion fails.
- **Verification:** Exit code `$LASTEXITCODE -eq 0`.
- **Resource Cost:** Low (executes in under 10 seconds).
- **Security Sensitivity:** Zero (hits public `/api/instance` diagnostic endpoint only).
