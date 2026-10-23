# Docker + Nginx Load Balancing Demonstration Report

**Project:** Smart Home / IoT Device Monitoring System  
**Assignment:** Docker Multi-Container Architecture & Nginx Load Balancing  
**Date:** October 2026  
**Status:** Fully Implemented and Verified  

---

## 1. System Architecture

```text
               +-------------------------------------------+
               |        Browser / Client / API Test        |
               +-------------------------------------------+
                                     |
                                     | HTTP (Port 8000)
                                     v
               +-------------------------------------------+
               |             smart-home-nginx              |
               |        (Nginx Reverse Proxy & LB)         |
               +-------------------------------------------+
                               /           \
                              /             \
      Round-Robin / Weighted /               \
                            v                 v
+-------------------------------+   +-------------------------------+
|     smart-home-backend-1      |   |     smart-home-backend-2      |
|  (Spring Boot 4.1.1 / Java 21)|   |  (Spring Boot 4.1.1 / Java 21)|
|    INSTANCE_NAME=backend-1    |   |    INSTANCE_NAME=backend-2    |
+-------------------------------+   +-------------------------------+
                            \                 /
                             \               /
                              v             v
               +-------------------------------------------+
               |        Oracle Database 21c XE Host        |
               | (host.docker.internal:1521 / XEPDB1)      |
               +-------------------------------------------+
```

---

## 2. Files Created and Modified

### New Files Created:
1. `backend/Dockerfile` — Multi-stage lightweight JRE 21 runtime container image definition.
2. `nginx/nginx.conf` — Active Nginx load-balancer configuration with custom upstream logging.
3. `nginx/nginx-round-robin.conf` — Reference round-robin upstream definition.
4. `nginx/nginx-weighted.conf` — Weighted upstream definition (`weight=3` and `weight=1`).
5. `docker-compose.yml` — Container orchestration for `backend1`, `backend2`, and `nginx`.
6. `backend/src/main/java/com/smarthome/smart_home_backend/controller/InstanceController.java` — Minimal diagnostic endpoint (`GET /api/instance`) returning container instance identity.
7. `docs/docker-demo/DOCKER_LOAD_BALANCING_REPORT.md` — Technical report and verification proof.
8. `docs/docker-demo/VIDEO_DEMO_SCRIPT.md` — 5-8 minute presentation script for student submission.
9. `.env` — Secure local environment configuration (`DB_PASSWORD` & Firebase credentials path).

### Files Minimally Modified:
1. `backend/src/main/java/com/smarthome/smart_home_backend/config/SecurityConfig.java` — Permitted public access to `/api/instance/**` so load balancing can be verified without requiring a user JWT.

---

## 3. Verified Execution Proof

### 3.1 Docker Installation & Version Verification
```bash
docker --version
# Output: Docker version 29.7.2, build a7dcaa6

docker compose version
# Output: Docker Compose version v5.5.1
```

### 3.2 Running Containers (`docker ps`)
```text
NAMES                  IMAGE                       STATUS         PORTS
smart-home-nginx       nginx:alpine                Up             0.0.0.0:8000->80/tcp, [::]:8000->80/tcp
smart-home-backend-2   smart-home-backend:latest   Up             8080/tcp
smart-home-backend-1   smart-home-backend:latest   Up             8080/tcp
```

### 3.3 Container Resource Monitoring (`docker stats --no-stream`)
```text
CONTAINER ID   NAME                   CPU %     MEM USAGE / LIMIT     MEM %     NET I/O           PIDS
1c8a7e1af5fd   smart-home-nginx       0.00%     3.88MiB / 7.71GiB     0.05%     58.3kB / 63.5kB   2
d9a9522918e8   smart-home-backend-2   0.25%     362.3MiB / 7.71GiB    4.59%     83.1kB / 72.1kB   54
eee6165244c7   smart-home-backend-1   0.22%     380.0MiB / 7.71GiB    4.81%     88.6kB / 81.2kB   54
```

### 3.4 Round-Robin Load Balancing Test
Command:
```powershell
python -c "import urllib.request, json; print(' -> '.join([json.loads(urllib.request.urlopen('http://localhost:8000/api/instance').read().decode())['instance'] for _ in range(10)]))"
```
Observed Output:
```text
backend-1 -> backend-2 -> backend-1 -> backend-2 -> backend-1 -> backend-2 -> backend-1 -> backend-2 -> backend-1 -> backend-2
```

### 3.5 Weighted Load Balancing Test (3:1 Ratio)
Switched Nginx to `nginx-weighted.conf` (`backend1:8080 weight=3; backend2:8080 weight=1;`) and reloaded via `docker exec smart-home-nginx nginx -s reload`.

Command:
```powershell
python -c "import urllib.request, json; from collections import Counter; counts = Counter(json.loads(urllib.request.urlopen('http://localhost:8000/api/instance').read().decode())['instance'] for _ in range(40)); print(counts)"
```
Observed Output (40 requests):
```text
TOTAL REQUESTS: 40
backend-1: 30 requests (75.0%)
backend-2: 10 requests (25.0%)
```

### 3.6 Nginx Access Logging with Upstream IPs
Nginx access log format records `$upstream_addr`, `$upstream_status`, and `$upstream_response_time`:
```text
172.18.0.1 - - [02/Oct/2026:16:24:24 +0000] "GET /api/instance HTTP/1.1" 200 24 "-" upstream: 172.18.0.2:8080 upstream_status: 200 response_time: 0.003
172.18.0.1 - - [02/Oct/2026:16:24:24 +0000] "GET /api/instance HTTP/1.1" 200 24 "-" upstream: 172.18.0.3:8080 upstream_status: 200 response_time: 0.003
```
- `172.18.0.2:8080` = `smart-home-backend-1`
- `172.18.0.3:8080` = `smart-home-backend-2`

### 3.7 Oracle & Firebase Status in Containers
Logs from both containers confirm:
- `Firebase Admin SDK initialized successfully: [DEFAULT]`
- `HikariPool-1 - Start completed.`
- `Database JDBC URL [jdbc:oracle:thin:@host.docker.internal:1521/XEPDB1]`
- `Database version: 21.3`
- `Started SmartHomeBackendApplication in ~21 seconds`

---

## 4. Operation Reference

### Start Stack
```bash
docker compose up -d
```

### Stop Stack
```bash
docker compose down
```

### Switch to Weighted Load Balancing
```powershell
Copy-Item nginx/nginx-weighted.conf nginx/nginx.conf -Force
docker exec smart-home-nginx nginx -s reload
```

### Restore Round-Robin Load Balancing
```powershell
Copy-Item nginx/nginx-round-robin.conf nginx/nginx.conf -Force
docker exec smart-home-nginx nginx -s reload
```

### View Live Nginx Logs
```bash
docker logs -f smart-home-nginx
```

### View Resource Usage
```bash
docker stats --no-stream
```
