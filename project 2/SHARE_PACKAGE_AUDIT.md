# DA3 Team Sharing Package — Security & Sanitation Audit

**Package Version:** DA3 Handover  
**Generated On:** October 2026  
**Target Audience:** Teammates working on the Comparison Engine and Frontend Integration  

---

## 1. Categories of Excluded Files & Artifacts

| Category | Excluded Items / Patterns | Justification |
| :--- | :--- | :--- |
| **Version Control Metadata** | `.git/`, `.gitattributes`, git commit histories, branch refs | Excluded to protect author's repository history and avoid conflict on recipient systems. |
| **Dependency Directories** | `node_modules/` across all frontend projects | Excluded to reduce archive bloat and avoid cross-platform native binary issues. Teammates run `npm install`. |
| **Build Outputs & Caches** | `backend/target/`, `dist/`, `build/`, `__pycache__/`, `.pytest_cache/` | Ephemeral build artifacts that should be compiled freshly on recipient machines. |
| **Secrets & Real Environment Files** | Real `.env` files, `.env.local` | Excluded to prevent credential leakage. Replaced with safe `.env.example` templates. |
| **Cloud Service Account Credentials** | Google Cloud / Firebase service-account JSON keys | Completely purged. No Google Cloud credentials exist in this archive. |
| **Temporary Files & Logs** | `*.log`, `*.tmp`, Windows thumbs, IDE caches | Local runtime artifacts excluded. |
| **Personal Paths & Names** | Hardcoded developer home directories (`C:\Users\<dev>`) | Sanitized to relative or generic placeholder paths. |
| **Previous Archive Zips** | `SmartHome_DBMS_Complete_Project.zip` | Excluded to avoid redundant duplicate archiving. |

---

## 2. Functionality Removed or Decoupled

### 2.1 Firebase Authentication Decoupling
- **Backend**:
  - Removed `com.google.firebase:firebase-admin` dependency from `pom.xml`.
  - Removed `FirebaseConfig.java`, `FirebaseAuthenticationFilter.java`, and `FirebaseUserDetails.java`.
  - Configured Spring Security to **Local Development Mode** (`permitAll()`) in `SecurityConfig.java`.
  - Updated `AuthController.java` to return a default local developer profile (`DA3 Teammate`, role `ADMIN`).
  - Updated `HomeAuthorizationService.java` to default to the local admin profile, allowing full access to all homes, devices, rooms, and rules.
- **Frontend**:
  - Removed `firebase` dependency from all `package.json` files (`frontend/`, `frontend-compare/`, `frontend-influx/`).
  - Removed `src/config/firebase.js` from all frontends.
  - Replaced `AuthContext.jsx` with a zero-dependency, local development AuthContext providing an active Admin user state.
  - Sanitized `api.js` in all frontends to perform clean REST calls without attempting to inject Firebase Bearer tokens.
  - Sanitized `visual.html` (3D Digital Twin Visualizer) to remove dynamic Firebase imports and acquire standard JSON headers.

---

## 3. Verified Functionality Available in the Package

- ✅ **Three Web Applications Fully Functional**:
  - `frontend/` on port `5173`: Oracle Smart Home Dashboard + 3D Visualizer + SQL Console.
  - `frontend-influx/` on port `5174`: InfluxDB Smart Home Dashboard + 3D Visualizer + Visual CRUD + Flux Console.
  - `frontend-compare/` on port `5175`: Side-by-side performance race, query latency comparison, and capability matrix.
- ✅ **Backend Comparison & InfluxDB Controllers**:
  - `CompareController.java` (`/api/compare/health`, `/api/compare/run`).
  - `InfluxDbController.java` (`/api/influx/health`, `/api/influx/telemetry`, `/api/influx/query`, `/api/influx/write`).
- ✅ **InfluxDB Time-Series Migration Engine**:
  - Docker Compose configuration for InfluxDB 2.x on `:8086`.
  - Ingestion scripts and sample telemetry dataset (20,120+ active points).
- ✅ **Clean Compilation**:
  - Backend compiles cleanly with JDK 21 (`BUILD SUCCESS`).
  - All three frontends build without errors with Vite 8.
