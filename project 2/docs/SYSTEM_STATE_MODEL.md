# System State Model — Canonical Taxonomy & Health Evaluation

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Deterministic state definitions, component observability matrix, health vs readiness  

---

## 1. System State Taxonomy

The system exists in exactly one of these seven macro states at any given moment:

```text
       +--------------------------------------------------------------+
       |                         UNKNOWN                              |
       |  (Initial agent state before running observation commands)   |
       +--------------------------------------------------------------+
                                       |
                                       v (Observe Environment)
       +--------------------------------------------------------------+
       |                         OBSERVED                             |
       |     (Containers running, listener detected, processes up)    |
       +--------------------------------------------------------------+
                                       |
                                       v (Run Validation Test)
       +--------------------------------------------------------------+
       |                         VERIFIED                             |
       | (HTTP 200 returned, DB queries succeed, round-robin proven)  |
       +--------------------------------------------------------------+
              |                                        |
              v (One replica down)                     v (DB down / 502)
       +--------------------+                   +---------------------+
       |      DEGRADED      |                   |       FAILED        |
       | (Partial service)  |                   | (Zero traffic flow) |
       +--------------------+                   +---------------------+
              |                                        |
              +-------------------+--------------------+
                                  | (Execute Runbook)
                                  v
                       +----------------------+
                       |     RECOVERING       |
                       |  (Restart in prog)   |
                       +----------------------+
```

| State Name | Definition | Operational Meaning | Required Agent Action |
| :--- | :--- | :--- | :--- |
| **UNKNOWN** | No fresh diagnostic commands have been executed. | State is uncertain; do not make assumptions. | Run `docker compose ps` and `curl` checks. |
| **OBSERVED** | Processes appear in process lists, but functionality unproven. | Containers are `Up`, but internal services may be initializing. | Wait for readiness or execute health probe. |
| **VERIFIED** | Active end-to-end HTTP/DB verification passed. | System is operational and healthy. | Safe to execute feature work or demonstrations. |
| **DEGRADED** | One replica failed or latency abnormal, but traffic still flows. | Subsystem redundancy lost. | Identify failing container, inspect logs, restart. |
| **FAILED** | Port refused, DB down, or 502 Bad Gateway returned. | Primary capability completely halted. | Enter [`docs/FAILURE_AND_RECOVERY.md`](FAILURE_AND_RECOVERY.md). |
| **RECOVERING** | Remediation command issued; waiting for stability. | In-flight state transition. | Observe logs until transition to VERIFIED or FAILED. |
| **DESIRED** | The target declarative architecture in Git. | Expected state defined in `docker-compose.yml`. | Align actual environment with DESIRED via Compose. |

---

## 2. Component Health & Readiness Matrix

| Component | Identifier | Observed Indicator | Verified Indicator | Degraded Indicator | Failed Indicator |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Host Oracle DB** | `host-oracle-xe` | Port `1521` in `netstat -ano` | Backend Hikari pool says `Start completed` | Slow queries (>5s) | Connection refused (`ORA-12541`) |
| **Backend 1** | `smart-home-backend-1` | `docker ps` shows Up | `GET :8000/api/instance` returns `backend-1` | High memory (>85%) | Container exited (exit code != 0) |
| **Backend 2** | `smart-home-backend-2` | `docker ps` shows Up | `GET :8000/api/instance` returns `backend-2` | High memory (>85%) | Container exited (exit code != 0) |
| **Nginx Proxy** | `smart-home-nginx` | Port `8000` listening on host | Round-robin alternates 1:1 on `:8000` | 502 returned on 1 of 2 requests | 502 Bad Gateway on all requests / Dead |
| **Firebase Auth** | `firebase-admin-sdk` | Service account JSON present | Log: `Firebase Admin SDK initialized successfully` | Expired tokens rejected | Service account missing / parse error |
| **React Frontend** | `react-vite-frontend` | Port `5173` listening on host | Browser renders landing page + login form | Static assets serve, API calls fail | Node process terminated |

---

## 3. How an Agent Determines Real System Health

Never claim a system is healthy simply because `docker ps` says `Up`. Follow this empirical evaluation sequence:

1. **Liveness Check (Cheapest):**
   ```powershell
   docker compose ps --format "{{.Name}}: {{.Status}}"
   ```
   *Expected:* All three containers report `Up ...`.
2. **Dual-Replica Readiness Check (Multi-Probe):**
   ```powershell
   1..2 | ForEach-Object { (Invoke-RestMethod -Uri 'http://localhost:8000/api/instance').instance }
   ```
   *Expected:* Consecutive responses must alternate across **both** `backend-1` and `backend-2`.
   > ⚠️ **Warning — Silent Failover Trap:** A single HTTP 200 response is **insufficient** to confirm cluster health. Nginx automatically fails over to the surviving replica if one backend crashes, masking an outage while the system is actually in a `DEGRADED` state with high failover latency.
3. **End-to-End Verification Check (Full):**
   ```powershell
   .\scripts\demo.ps1
   ```
   *Expected:* All 7 checks report `[PASS]` and script exits with code 0.
