# Standard Operating Procedures & Runbooks

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Reusable, deterministic, step-by-step procedures for humans and agents  

---

## Runbook Index

- **RB-01:** Full Docker Cluster Startup
- **RB-02:** Full Docker Cluster Graceful Shutdown
- **RB-03:** Automated Health & Load-Balancing Verification
- **RB-04:** Dynamic Load-Balancing Reconfiguration (Round-Robin <-> Weighted)
- **RB-05:** Local Development Launch (Host Native, No Docker)
- **RB-06:** Container Restart & Resynchronization
- **RB-07:** Full Cluster Disaster Recovery & Reset

---

### RB-01: Full Docker Cluster Startup

- **PURPOSE:** Launch `backend1`, `backend2`, and `nginx` containers attached to host Oracle Database.
- **PRECONDITIONS:**
  1. Docker Desktop is running (`docker --version`).
  2. Oracle Database 21c XE is listening on host port `1521` (`netstat -ano | findstr :1521`).
  3. Environment variables present in `.env` (`DB_PASSWORD`, `FIREBASE_CREDENTIALS_PATH`).
- **ACTION:**
  ```powershell
  cd C:\Users\akhil\OneDrive\Desktop\Projects\dbms
  docker compose up -d
  ```
- **VERIFY:**
  ```powershell
  docker compose ps
  ```
- **EXPECTED RESULT:** `smart-home-nginx`, `smart-home-backend-1`, and `smart-home-backend-2` are `Up`.
- **FAILURE PATH:** If containers exit immediately, execute `docker compose logs` to check database password or Firebase JSON path.

---

### RB-02: Full Docker Cluster Graceful Shutdown

- **PURPOSE:** Terminate all containers and virtual networks cleanly without corrupting host database state.
- **PRECONDITIONS:** None.
- **ACTION:**
  ```powershell
  docker compose down
  ```
- **VERIFY:**
  ```powershell
  docker ps -q --filter "name=smart-home"
  ```
- **EXPECTED RESULT:** Output is empty.
- **FAILURE PATH:** If containers hang during SIGTERM: `docker compose down -t 5`.

---

### RB-03: Automated Health & Load-Balancing Verification

- **PURPOSE:** Execute deterministic 7-stage test suite and capture empirical proof.
- **PRECONDITIONS:** RB-01 complete.
- **ACTION:**
  ```powershell
  .\scripts\demo.ps1
  ```
- **VERIFY:** Check console output for 7 green `[PASS]` badges.
- **EXPECTED RESULT:** Exit code 0, round-robin alternates 1:1, weighted test distributes ~75%/25%.
- **FAILURE PATH:** See [`docs/FAILURE_AND_RECOVERY.md`](FAILURE_AND_RECOVERY.md).

---

### RB-04: Dynamic Load-Balancing Reconfiguration

- **PURPOSE:** Switch Nginx between round-robin and weighted load balancing on the fly.
- **PRECONDITIONS:** Containers running.
- **ACTION (Switch to Weighted):**
  ```powershell
  Copy-Item nginx/nginx-weighted.conf nginx/nginx.conf -Force
  docker exec smart-home-nginx nginx -s reload
  ```
- **ACTION (Restore Round-Robin):**
  ```powershell
  Copy-Item nginx/nginx-round-robin.conf nginx/nginx.conf -Force
  docker exec smart-home-nginx nginx -s reload
  ```
- **VERIFY:** Run `python -c "import urllib.request, json; print([json.loads(urllib.request.urlopen('http://localhost:8000/api/instance').read().decode())['instance'] for _ in range(4)])"`.
- **EXPECTED RESULT:** Alternating sequence indicates round-robin; repeated `backend-1` indicates weighted.

---

### RB-05: Local Development Launch (Host Native)

- **PURPOSE:** Run backend and frontend on host machine for rapid code editing without container rebuilds.
- **PRECONDITIONS:** Port `8080` and `5173` free on host. (Stop Docker containers if running: `docker compose down`).
- **ACTION (Backend):**
  ```powershell
  cd backend
  $env:DB_PASSWORD="SmartHomeDev123"
  $env:ADMIN_GOOGLE_EMAIL="akhilesh07vaidya@gmail.com"
  $env:GOOGLE_APPLICATION_CREDENTIALS="C:\Users\akhil\OneDrive\Desktop\firebase-secrets\smart-home-service-account.json"
  .\mvnw.cmd spring-boot:run
  ```
- **ACTION (Frontend):**
  ```powershell
  cd frontend
  npm run dev
  ```
- **VERIFY:** Open `http://localhost:5173` in browser.

---

### RB-06: Container Restart & Resynchronization

- **PURPOSE:** Restart a single degraded backend container without bringing down the cluster.
- **ACTION:**
  ```powershell
  docker restart smart-home-backend-1
  ```
- **VERIFY:** Check `docker logs -f smart-home-backend-1` until `Started SmartHomeBackendApplication` appears.

---

### RB-07: Full Cluster Disaster Recovery & Reset

- **PURPOSE:** Recover from corrupted container state, failed rebuilds, or network deadlocks.
- **ACTION:**
  ```powershell
  docker compose down -v
  docker build -t smart-home-backend:latest ./backend
  docker compose up -d --force-recreate
  Start-Sleep -Seconds 15
  .\scripts\demo.ps1
  ```
- **VERIFY:** `demo.ps1` completes with all tests passing.
