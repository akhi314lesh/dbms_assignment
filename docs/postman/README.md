# SmartHome API — Postman Test Suite & Environment Setup

This directory contains the complete automated test suite and environment configuration for the **SmartHome REST API** (Phase 9.3).

---

## 1. Files Included

| File | Description |
| :--- | :--- |
| `SmartHome-API.postman_collection.json` | Complete Postman collection containing **13 folders**, **62 requests**, and **127 automated test assertions**. |
| `SmartHome-Local.postman_environment.json` | Postman environment definition with variable placeholders (`baseUrl`, `adminToken`, `userToken`, etc.). |
| `README.md` | Usage guide, security architecture, token setup instructions, and execution workflows. |
| `PHASE_9_3_REPORT.md` | Comprehensive Phase 9.3 verification report with full test results, findings, and Oracle counts. |

---

## 2. Security & Authentication Architecture

All authenticated API endpoints in SmartHome require a valid, cryptographically verified Firebase ID token passed in the `Authorization` header:

```http
Authorization: Bearer {{adminToken}}
```
or
```http
Authorization: Bearer {{userToken}}
```

### Critical Security Rules:
- **Zero Authentication Bypasses**: The API never accepts dummy, mock, or synthetic tokens. Every token is verified by the backend via Firebase Admin SDK.
- **Role Isolation**:
  - `adminToken`: Bound to `ADMIN` role (`akhilesh07vaidya@gmail.com`). Has universal visibility across all 5 homes, access to user administration, and full SQL Console DML capability.
  - `userToken`: Bound to `USER` role (`sophia.chen@smarthome.io`, Sophia Chen). Has restricted visibility scoped strictly to Home 1 (`Sanctuary Haven`), SELECT-only SQL console access, and is rejected with `403 Forbidden` on all foreign homes and devices.
- **Never Committed**: `SmartHome-API.postman_collection.json` and `SmartHome-Local.postman_environment.json` contain **no hardcoded tokens**. Tokens must be provided locally into the environment.

---

## 3. Obtaining Firebase ID Tokens via Local UI Helper

A temporary development-only token exporter has been integrated into the frontend application (`http://localhost:5173`).

### Exact Locations in the UI:
Once signed in with your Google account on `http://localhost:5173`, the helper is accessible in **two locations**:

1. **Option 1: Floating Widget (Bottom-Right Corner)**:
   - Located at the bottom-right corner of every page when authenticated.
   - Labeled `[DEV ONLY] Postman Token Helper`.
   - Displays your active account email, your detected Oracle role (`ADMIN` or `USER`), and the target Postman environment variable (`adminToken` vs `userToken`).
   - Click the prominent button:
     - If Admin: **`📋 Copy adminToken to Clipboard`**
     - If User: **`📋 Copy userToken to Clipboard`**
   - It internally executes `await auth.currentUser.getIdToken(true)` to force a fresh token (valid for a full 1-hour testing window) and copies the JWT string to your clipboard.

2. **Option 2: Top Navigation Bar Button**:
   - Located on the landing page header directly to the left of the **Sign Out** button.
   - Button labeled: **`📋 [DEV] Copy Token`**.
   - Clicking it immediately copies the fresh ID token to your clipboard and shows a confirmation alert.

### Step-by-Step Instructions to Populate Postman:

1. **Get `adminToken`**:
   - Navigate to `http://localhost:5173`.
   - Sign in with your administrator account (`akhilesh743@gmail.com` / `akhilesh07vaidya@gmail.com`).
   - Click **`📋 Copy adminToken to Clipboard`** in the bottom-right widget.
   - In Postman, paste the token into the `adminToken` variable.
2. **Get `userToken`**:
   - Click **Sign Out** in the top navbar.
   - Sign in with your user account (`sophia.chen@smarthome.io` or configured test user).
   - Click **`📋 Copy userToken to Clipboard`**.
   - In Postman, paste the token into the `userToken` variable.

---

## 4. Postman Environment Variables

Import `SmartHome-Local.postman_environment.json` into Postman. The variables are configured as follows:

| Variable | Type | Default Value | Purpose |
| :--- | :--- | :--- | :--- |
| `baseUrl` | default | `http://localhost:8080` | Base URL of the Spring Boot backend. |
| `adminToken` | secret | `""` | Fresh Firebase ID token for ADMIN. |
| `userToken` | secret | `""` | Fresh Firebase ID token for USER. |
| `accessibleHomeId` | default | `1` | Home ID accessible to Sophia Chen (`Sanctuary Haven`). |
| `foreignHomeId` | default | `2` | Home ID inaccessible to Sophia Chen (`Alpine Ridge Retreat`). |
| `testDeviceId` | default | `1` | Test device located in Home 1 (`Living Room Central Hub`). |
| `foreignDeviceId` | default | `7` | Foreign device located in Home 2 (`Great Room Ambient Light`). |
| `testAlertId` | default | `2` | Active alert in Home 1 used for PL/SQL acknowledgement tests. |
| `foreignAlertId` | default | `3` | Foreign alert in Home 2 used for 403 authorization tests. |
| `createdReadingId` | default | `""` | Dynamically set at runtime by sensor reading test. |

---

## 5. Running the Collection in Postman Desktop App

1. Open Postman Desktop.
2. Click **Import** (top left) and select:
   - `docs/postman/SmartHome-API.postman_collection.json`
   - `docs/postman/SmartHome-Local.postman_environment.json`
3. In the top right environment dropdown, select **SmartHome-Local**.
4. Click the environment quick-look (eye icon), edit `SmartHome-Local`, and paste the copied tokens into `adminToken` and `userToken` current values.
5. In the left sidebar, click the **SmartHome API - Phase 9.3 Verification Suite** collection.
6. Click **Run collection**.
7. Ensure all 13 folders are checked and click **Run SmartHome API...**.
8. Observe all tests pass with 0 failures!

---

## 6. Running via Newman CLI (Automated Local Run)

You can run the entire suite locally using Newman:

```powershell
npx newman run docs/postman/SmartHome-API.postman_collection.json `
    -e docs/postman/SmartHome-Local.postman_environment.json `
    --env-var "adminToken=<YOUR_ADMIN_TOKEN>" `
    --env-var "userToken=<YOUR_USER_TOKEN>" `
    --color off
```

---

## 7. Collection Structure (13 Folders)

1. **`01 Authentication`** — Validates `GET /api/auth/me` identity resolution, role assignment (`ADMIN` vs `USER`), and Oracle user mapping.
2. **`02 Homes`** — Tests universal home listing for Admin (5 homes), row-level access restriction for User (1 home), and 403 rejection on foreign home.
3. **`03 Rooms`** — Tests global rooms (22 rooms) vs home-scoped rooms, and 403 on foreign home rooms.
4. **`04 Devices`** — Tests global devices (45 devices), home-scoped devices, detailed DTO resolution (`homeId`, `homeName`), and full Device Status Lifecycle (toggle to OFFLINE, verify, restore to ONLINE, and 403 on foreign device).
5. **`05 Device Categories`** — Tests all five Phase 9.2 subtype endpoints (`SMART_LIGHT`, `THERMOSTAT`, `TEMPERATURE_SENSOR`, `MOTION_SENSOR`, `CAMERA`), strict subtype validation, user scoping, and 400 Bad Request on invalid subtype.
6. **`06 Alerts`** — Tests alert querying, user scoping, PL/SQL stored procedure alert acknowledgement (`sp_acknowledge_alert`), automated state restoration, and 403 on foreign alert.
7. **`07 Automation`** — Tests automation rule listing for Admin and home/user-scoped listing for User.
8. **`08 Notification Preferences`** — Tests global preference management vs user-scoped preferences.
9. **`09 Sensor Readings`** — Tests reading retrieval, user scoping, stored procedure execution (`sp_record_sensor_reading`), and immediate reversible cleanup.
10. **`10 Health Report`** — Tests Oracle PL/SQL package health report generation (`sp_home_device_health_report`), Admin access, User accessible home access, and 403 rejection on foreign home.
11. **`11 SQL Console`** — Tests dual security engine: Admin DML allowed, User SELECT allowed, User INSERT/UPDATE/DELETE rejected, sensitive tables (`USERS`, `HOME_ACCESS`) blocked, destructive DDL (`DROP`) prohibited, and reversible DML.
12. **`12 Authorization / Security`** — Tests administrative user directory protection (`GET /api/users` 403 for regular users), `HOME_ACCESS` directory protection, and foreign device modification denial.
13. **`13 Validation / Error Handling`** — Tests 401 Unauthorized (missing header), 400 Bad Request (invalid status enum), and 404 Not Found (nonexistent device, home, alert).
