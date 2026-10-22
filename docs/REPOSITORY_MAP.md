# Agent-Friendly Repository Map

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Exhaustive, machine-legible directory and file ownership matrix  

---

## 1. Top-Level Directory Inventory

| Path | Purpose & Ownership | Inputs | Outputs | Safe to Modify? | Canonical Source | Verification Method |
| :--- | :--- | :--- | :--- | :-: | :--- | :--- |
| `backend/` | Spring Boot 4.1.1 REST API & Business Logic | Java source, `pom.xml` | Executable JAR (`target/*.jar`) | 🟡 Caution | Java files in `src/main/` | `.\mvnw.cmd test` / build |
| `frontend/` | React 19 SPA, Tailwind CSS 4, Three.js Digital Twin | React JSX, CSS, Vite config | Static bundle (`dist/`) | 🟢 Safe | Components in `src/` | `npm run build` |
| `database/` | Oracle 21c XE Relational Schemas, PL/SQL, Triggers | SQL / PL/SQL files | Relational state in Oracle | 🟠 Destructive | SQL scripts `01_` to `07_` | SQL*Plus / SQL Console API |
| `nginx/` | Reverse Proxy, Round-Robin, Weighted Upstream Configs| Configuration files | Ingress routing on `:8000` | 🟢 Safe | `nginx/nginx.conf` | `docker exec ... nginx -t` |
| `scripts/` | Automated test suites & `demo.ps1` dashboard | PowerShell scripts | Console test evidence | 🟢 Safe | `scripts/demo.ps1` | `.\scripts\demo.ps1` |
| `docs/` | Canonical architectural specifications & runbooks | Markdown files | Cognitive orientation for agents| 🟢 Safe | `docs/` markdown graph | Markdown lint / manual audit |

---

## 2. Key Critical Files

| File Path | Role & Invariants | Safe to Modify? | Rollback Mechanism |
| :--- | :--- | :-: | :--- |
| `docker-compose.yml` | Declarative container cluster topology. Uses `smart-home-backend:latest` and `host.docker.internal`. | 🟡 Caution | `git checkout docker-compose.yml` |
| `backend/Dockerfile` | Java 21 Temurin Alpine image specification. | 🟡 Caution | `git checkout backend/Dockerfile` |
| `backend/pom.xml` | Maven dependency management (Java 21, Spring Boot 4.1.1, Oracle JDBC, Firebase Admin). | 🟡 Caution | `git checkout backend/pom.xml` |
| `backend/src/main/resources/application.properties` | Central runtime properties (JDBC URL, username, password fallback, CORS origins). | 🟡 Caution | `git checkout backend/src/.../application.properties` |
| `backend/.../config/SecurityConfig.java` | Spring Security filter chain, stateless JWT verification, CORS policy, public route whitelist. | 🟡 Caution | `git checkout backend/.../SecurityConfig.java` |
| `backend/.../controller/InstanceController.java` | Diagnostic endpoint returning `INSTANCE_NAME` environment variable (`backend-1` / `backend-2`). | 🟢 Safe | Delete or revert file |
| `frontend/src/services/api.js` | Centralized Axios instance resolving `VITE_API_URL` and injecting Firebase Bearer tokens. | 🟢 Safe | `git checkout frontend/src/services/api.js` |
| `frontend/visual.html` | Three.js 3D Digital Twin visualizer and spatial estate rendering engine. | 🟢 Safe | `git checkout frontend/visual.html` |
| `.env` | Local uncommitted secrets (`DB_PASSWORD`, `FIREBASE_CREDENTIALS_PATH`). Gitignored. | 🟢 Safe | Recreate from `.env.example` |
