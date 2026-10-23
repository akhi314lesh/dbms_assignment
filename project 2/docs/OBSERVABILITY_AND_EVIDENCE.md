# Observability & Evidence Model

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Telemetry surfaces, health indicators, log interpretation, and evidence hierarchy  

---

## 1. The Evidence Hierarchy

Agents and humans must never confuse an unverified assumption with empirical fact. All operational statements must be classified into one of these four epistemic confidence tiers:

```text
[ TIER 4: INFERENCE ]     "The container probably crashed because memory was high."
       |
[ TIER 3: CLAIM ]         "The backend is healthy and running on port 8080."
       |
[ TIER 2: OBSERVATION ]   "docker ps shows smart-home-backend-1 Up 5 minutes."
       |
[ TIER 1: VERIFICATION ]  "curl :8000/api/instance returned HTTP 200 with {'instance':'backend-1'} in 0.003s."
```

| Tier | Name | Definition | Trust Level | Acceptable as Proof? |
| :-: | :--- | :--- | :--- | :-: |
| **1** | **VERIFICATION** | An active, end-to-end operational test with deterministic status code and expected payload. | **Absolute** | **YES** |
| **2** | **OBSERVATION** | A raw inspection command (e.g. process list, file existence check, log dump). | **High** | With context |
| **3** | **CLAIM** | A statement in documentation or prompt asserting system state without fresh output. | **Low** | **NO** |
| **4** | **INFERENCE** | A deduction or hypothesis attempting to explain an observation. | **Hypothetical** | **NO** |

---

## 2. Observability Surfaces

### A. Reverse Proxy Telemetry (Nginx Access Log)
- **Path inside container:** `/var/log/nginx/access.log`
- **Inspection Command:** `docker logs --tail 20 smart-home-nginx`
- **Custom Format:**
  ```text
  $remote_addr - $remote_user [$time_local] "$request" $status $body_bytes_sent "$http_referer" "$http_user_agent" upstream: $upstream_addr upstream_status: $upstream_status response_time: $upstream_response_time
  ```
- **Key Indicators:**
  - `upstream: 172.18.0.2:8080` -> Request routed to `smart-home-backend-1`.
  - `upstream: 172.18.0.3:8080` -> Request routed to `smart-home-backend-2`.
  - `upstream_status: 200` -> Target backend responded successfully.
  - `upstream_status: 502` -> Target backend unreachable or crashed.

### B. Connection Pool Telemetry (HikariCP)
- **Log Stream:** `docker logs smart-home-backend-1` / `smart-home-backend-2`
- **Key Signatures:**
  - `HikariPool-1 - Added connection oracle.jdbc.driver.T4CConnection`: Connection successfully established to Oracle DB.
  - `HikariPool-1 - Start completed`: Pool initialized (default 10 connections).
  - `Connection is not available, request timed out`: Database pool exhausted or Oracle XE listener unresponsive.

### C. Container Resource Metrics
- **Inspection Command:** `docker stats --no-stream`
- **Expected Baseline Values:**
  - `smart-home-nginx`: ~4 MiB RAM, <0.05% CPU, 2 PIDs.
  - `smart-home-backend-1`: 360–480 MiB RAM, <1% idle CPU, ~54 PIDs.
  - `smart-home-backend-2`: 360–480 MiB RAM, <1% idle CPU, ~54 PIDs.
  - *Alert Threshold:* Memory usage > 1.2 GiB or CPU pegged at 100% indicates runaway garbage collection or infinite loop.

---

## 3. Four-Tier Status Separation

Never conflate operational stages:

1. **Operation Completed:** Command returned exit code 0 (e.g. `docker compose up -d`). *Does not guarantee application readiness.*
2. **Operation Succeeded:** The service bound to its port and initialized beans (e.g. Tomcat started on 8080).
3. **System Is Healthy:** Synthetic probes return valid data across all endpoints.
4. **System Is Performing as Intended:** Round-robin or weighted ratio matches mathematical expectations under load.
