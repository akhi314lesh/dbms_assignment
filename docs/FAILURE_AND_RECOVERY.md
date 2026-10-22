# Failure Catalog & Disaster Recovery

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Symptom-to-action diagnostic trees, safe rollback paths, root cause signatures  

---

## 1. Quick-Triage Decision Tree

```text
Problem: Request to http://localhost:8000 fails
  |
  +---> Connection Refused on :8000?
  |       --> FIRST CHECK: docker compose ps
  |       --> CAUSE: Nginx container down.
  |       --> SAFE ACTION: docker start smart-home-nginx
  |
  +---> HTTP 502 Bad Gateway?
  |       --> FIRST CHECK: docker logs --tail 20 smart-home-nginx
  |       --> CAUSE: Backends crashed or initializing.
  |       --> SECOND CHECK: docker logs --tail 20 smart-home-backend-1
  |       --> SAFE ACTION: Wait 15s or restart backend: docker restart smart-home-backend-1
  |
  +---> HTTP 401 Unauthorized?
  |       --> FIRST CHECK: Does request include 'Authorization: Bearer <token>'?
  |       --> CAUSE: Missing or expired Firebase JWT token. Normal for secure endpoints.
  |       --> SAFE ACTION: Use public endpoint /api/instance or generate fresh Firebase token.
  |
  +---> Backend logs show ORA-12541 / Connection Refused?
          --> FIRST CHECK: netstat -ano | findstr :1521
          --> CAUSE: Oracle XE service stopped on Windows host.
          --> SAFE ACTION: Start-Service OracleServiceXE
```

---

## 2. Failure Signatures Catalog

### Failure 1: Nginx Returns 502 Bad Gateway
- **Symptom:** Client receives `502 Bad Gateway` from `http://localhost:8000`.
- **Root Cause:** Nginx cannot establish TCP socket to `backend1:8080` or `backend2:8080`.
- **First Check:** `docker compose ps` (Check if backend containers are `Up`).
- **Second Check:** `docker logs --tail 25 smart-home-backend-1` (Check if Spring Boot threw exception during startup).
- **Safe Action:** If backends are still in early startup phase (takes ~20s), wait 10 seconds. If crashed, inspect logs for DB password or Firebase errors.
- **Recovery:** `docker compose restart backend1 backend2`.
- **Escalation:** Rebuild backend JAR and container image via [`docs/OPERATIONS.md`](OPERATIONS.md#rb-07-full-cluster-disaster-recovery--reset).

---

### Failure 2: Oracle Database Connection Refusal (`ORA-12541` / `ConnectException`)
- **Symptom:** Backend container crashes on startup with `HikariPool-1 - Exception during pool initialization`.
- **Root Cause:** Oracle Database 21c XE is not listening on host port `1521` or `host.docker.internal` failed to resolve.
- **First Check:** On Windows host: `netstat -ano | findstr :1521` (Must show `0.0.0.0:1521 LISTENING`).
- **Second Check:** Verify `extra_hosts: ["host.docker.internal:host-gateway"]` in `docker-compose.yml`.
- **Safe Action:** Start host Oracle service: `Start-Service OracleServiceXE` and `Start-Service OracleOraDB21Home1TNSListener`.
- **Recovery:** Once port 1521 is verified listening, run `docker compose restart backend1 backend2`.

---

### Failure 3: Firebase Admin SDK Authentication Failure (`401 Unauthorized`)
- **Symptom:** Backend logs display `Failed to verify token` or `Service account file not found`.
- **Root Cause:** Service account JSON file missing, unreadable, or expired client JWT.
- **First Check:** Inspect host path `C:\Users\akhil\OneDrive\Desktop\firebase-secrets\smart-home-service-account.json`.
- **Second Check:** Verify volume bind mount in `docker-compose.yml` (`/app/firebase-secrets/...:ro`).
- **Safe Action:** Re-export client ID token from Firebase Web SDK (tokens expire after 1 hour).
- **Recovery:** Check that `GOOGLE_APPLICATION_CREDENTIALS` points to container path `/app/firebase-secrets/smart-home-service-account.json`.

---

### Failure 4: Weighted Load Balancing Stays in Weighted Mode
- **Symptom:** Requests continuously route ~75% to `backend-1` instead of alternating 1:1.
- **Root Cause:** A test script crashed before executing the cleanup/restore step.
- **First Check:** View `nginx/nginx.conf` and search for `weight=3`.
- **Safe Action:** Restore reference round-robin config:
  ```powershell
  Copy-Item nginx/nginx-round-robin.conf nginx/nginx.conf -Force
  docker exec smart-home-nginx nginx -s reload
  ```
- **Verification:** Run `.\scripts\test-round-robin.ps1` to confirm 1:1 alternation.

---

### Failure 5: Host Port 8080 Conflict
- **Symptom:** Attempting to start host Spring Boot fails with `Port 8080 is already in use`.
- **Root Cause:** A leftover background Java process or local development server is holding port 8080.
- **First Check:** `netstat -ano | findstr :8080` (identify PID).
- **Safe Action:** If safe to terminate: `Stop-Process -Id <PID> -Force`.
- **Recovery:** Re-run local Maven launch.
---

### Failure 6: Silent Upstream Failover (HTTP 200 with Hidden Degradation)
- **Symptom:** Client receives `HTTP 200 OK` from `http://localhost:8000`, but response latency periodically spikes (~800ms) and consecutive requests return only a single instance name (`backend-1` or `backend-2`).
- **Root Cause:** One backend container crashed or was stopped. Nginx attempted connection to the dead upstream, encountered `113: Host is unreachable`, logged `upstream_status: 502, 200`, and silently failed over to the remaining live replica.
- **First Check:** `docker compose ps` (Check if container count is 3 and all status values are `Up`).
- **Second Check:** `docker logs --tail 15 smart-home-nginx` (Look for compound status: `upstream_status: 502, 200` and `connect() failed`).
- **Safe Action:** Identify the exited container and execute `docker start <container_name>` (e.g. `docker start smart-home-backend-1`).
- **Verification:** Run `.\scripts\test-round-robin.ps1` to confirm both replicas are alternating 1:1 and latency returns to <5ms.
- **Operational Rule:** **Never rely on a single HTTP 200 response to declare cluster health.** A single probe cannot distinguish between a fully redundant cluster (`VERIFIED`) and a single-replica survivor (`DEGRADED`).
