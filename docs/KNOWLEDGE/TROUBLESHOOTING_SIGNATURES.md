# Empirical Troubleshooting Signatures

Known log patterns and their exact remediations:

- **Signature:** `HikariPool-1 - Exception during pool initialization: IO Error: The Network Adapter could not establish the connection`
  - **Meaning:** Host Oracle listener is stopped or `host.docker.internal` did not route to host port 1521.
  - **Remedy:** Run `netstat -ano | findstr :1521` on host and verify `0.0.0.0:1521 LISTENING`. Restart Oracle XE service if needed.

- **Signature:** `502 Bad Gateway` in Nginx access log with `upstream_status: 502`
  - **Meaning:** Target backend container is either still booting (takes ~20 seconds) or encountered a fatal startup error.
  - **Remedy:** Check `docker logs smart-home-backend-1`. Wait for `Started SmartHomeBackendApplication`.

- **Signature:** `Full authentication is required to access this resource` (HTTP 401)
  - **Meaning:** Normal Spring Security behavior on protected routes (`/api/homes`, `/api/devices`, etc.) when no Firebase Bearer token is passed.
  - **Remedy:** Use `/api/instance` for unauthenticated diagnostic testing, or pass a valid Firebase Bearer token.\n- **Signature:** upstream_status: 502, 200 in Nginx access log with connect() failed (113: Host is unreachable)
  - **Meaning:** Silent upstream failover. One backend container is stopped or crashed; Nginx attempted to route to it, failed over to the remaining live replica, and returned 200 to the client with elevated response latency (~800ms).
  - **System State:** DEGRADED (Partial failure; redundancy lost).
  - **Remedy:** Run docker compose ps to identify which container exited, and execute docker start <container_name>.\n