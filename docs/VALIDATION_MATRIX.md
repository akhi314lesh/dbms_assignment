# System Validation Matrix

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Exhaustive, empirical verification matrix across all system capabilities  

---

## Complete Verification Matrix

| Capability / Flow | Precondition | Test Command | Trustworthy Evidence | Expected Result | Failure Signature | Fast Recovery Path |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Docker Engine & Compose** | Docker Desktop running | `docker --version; docker compose version` | Terminal output strings | Versions printed without error | `docker: command not found` or daemon error | Start Docker Desktop application |
| **Host Oracle DB Port** | Oracle XE service running | `netstat -ano \| findstr :1521` | TCP socket listing | `0.0.0.0:1521 LISTENING` | No output or port missing | `Start-Service OracleServiceXE` |
| **Container Cluster Status** | Stack started via Compose | `docker compose ps --format "{{.Name}}: {{.Status}}"` | Formatted status list | All 3 containers report `Up ...` | `Exit 1` or container missing | `docker compose logs` -> restart failed container |
| **Container Identification** | Cluster healthy | `curl.exe -s http://localhost:8000/api/instance` | JSON response payload | `{"instance":"backend-1"}` or `{"instance":"backend-2"}` | Connection refused or 502 Bad Gateway | Check Nginx logs: `docker logs smart-home-nginx` |
| **Round-Robin Load Balancing** | Both backends up | `.\scripts\test-round-robin.ps1` | 10-request console trace | Alternates 1:1 between backend-1 and backend-2 | All requests hit single container | Check Nginx upstream block in `nginx/nginx.conf` |
| **Weighted Traffic Distribution** | Both backends up | `.\scripts\test-weighted.ps1` | 40-request aggregate counter | ~75% backend-1 (~30 req), ~25% backend-2 (~10 req) | Distribution 50/50 or 100/0 | Verify `weight=3` in `nginx-weighted.conf`; reload |
| **Nginx Upstream Access Logging** | Requests sent through :8000 | `docker logs --tail 10 smart-home-nginx` | Log lines with `$upstream_addr` | Lines show `172.18.0.2:8080` and `172.18.0.3:8080` | Upstream field missing or 502 status | Check log format `upstream_log` in `nginx.conf` |
| **Live Resource Health** | Containers running | `docker stats --no-stream` | Resource table output | Mem: Nginx <10MB, Backends 350-500MB; CPU <2% | Mem >1.2GB or CPU 100% | Inspect thread dumps or restart container |
| **Security & JWT Guard** | Backend running | `curl.exe -s -o /dev/null -w "%{http_code}" http://localhost:8000/api/homes` | HTTP status code | `401` Unauthorized | Status `200` (security breach) or `500` | Inspect `SecurityConfig.java` and filter chain |
| **Automated Demonstration** | Stack running | `.\scripts\demo.ps1` | Exit code & ASCII dashboard | All 7 checks report `[PASS]`, exit code 0 | Script terminates with `[FAIL]` | Read specific failed section in dashboard output |
