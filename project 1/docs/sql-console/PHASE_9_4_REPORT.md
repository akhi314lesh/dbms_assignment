# Phase 9.4 — Real SQL Query Templates, Scoped User DML & PL/SQL Execution Report

**Author:** Antigravity AI Engineering Assistant  
**Date:** September 18, 2026  
**Status:** **100% COMPLETE & VERIFIED (All 15 Unit/Integration Tests Passed, Postman Collection Updated, Oracle Baseline Row Count Preserved)**  
**Target Environment:** Local Spring Boot (`:8080`) + Oracle Database 21c XE (`localhost:1521/XEPDB1`) + Vite Frontend (`:5173`)

---

## 1. Final SQL & PL/SQL Permission Model

The SQL Console permission model provides authorized administrative database management while isolating regular user operations to homes accessible via the `HOME_ACCESS` associative entity:

```
Authenticated User (JWT / SecurityContext)
                   │
         Is Role == 'ADMIN'?
        /                   \
      YES                    NO (USER)
       │                      │
Full Database DML       DML Scoped to HOME_ACCESS
(Subject to Global      • SELECT: Scoped inline views
 Prohibitions)          • INSERT: Parent ID validation
                        • UPDATE: Scope predicate injection
                        • DELETE: WHERE clause + scope predicate
                        • IDENTITY: USERS / HOME_ACCESS strictly blocked
                        • HOMES: Direct INSERT blocked
       │                      │
       └──────────┬───────────┘
                  │
        Approved PL/SQL Invocations
        (FN_CALCULATE_DEVICE_UPTIME, FN_CHECK_DEVICE_ACCESS,
         SP_ACKNOWLEDGE_ALERT, SP_HOME_DEVICE_HEALTH_REPORT,
         SP_RECORD_SENSOR_READING, TEST_TRIGGER_CYCLE)
                  │
       Global Prohibitions Enforced
       (DROP, ALTER, TRUNCATE, CREATE, RENAME, COMMENT,
        GRANT, REVOKE, EXEC, BEGIN, DECLARE, DBMS_*, UTL_*,
        SYS.*, DBA_*, Multi-Statements)
```

---

## 2. Role Capabilities

| Capability | Administrator (`ADMIN`) | Regular User (`USER`) |
| :--- | :--- | :--- |
| **`SELECT`** | Universal access across all tables | Isolated to accessible homes via subquery rewriting |
| **`INSERT`** | Full insertion across domain tables | Allowed only when parent `home_id`/`room_id`/`device_id` belongs to an accessible home |
| **`UPDATE`** | Full mutation across domain tables | Allowed only for rows within accessible homes (scope predicate injected) |
| **`DELETE`** | Allowed with explicit `WHERE` clause | Allowed only for rows within accessible homes with explicit `WHERE` clause |
| **Identity Tables (`USERS`, `HOME_ACCESS`, etc.)** | Full administrative DML | **STRICTLY BLOCKED** (Read & Write) |
| **Direct `INSERT INTO HOMES`** | Allowed | **BLOCKED** (Must use atomic `POST /api/homes` flow) |
| **PL/SQL Functions & Procedures** | All approved allowlist routines | Approved routines scoped to accessible homes |
| **Trigger Testing** | Controlled cycle test | Controlled cycle test |
| **Arbitrary `EXEC` / Anonymous PL/SQL** | **BLOCKED** | **BLOCKED** |
| **DDL / System Catalogs (`DROP`, `SYS.*`)** | **BLOCKED** | **BLOCKED** |

---

## 3. Allowed vs. Blocked USER Tables

### Allowed USER Tables (Home-Scoped)
1. `HOMES` (`SELECT`, `UPDATE` on accessible homes)
2. `ROOMS` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
3. `DEVICES` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
4. `SMART_LIGHTS` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
5. `THERMOSTATS` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
6. `TEMPERATURE_SENSORS` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
7. `MOTION_SENSORS` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
8. `CAMERAS` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
9. `SENSOR_READINGS` (`SELECT`, `INSERT`, `DELETE`)
10. `ALERTS` (`SELECT`, `UPDATE`, `DELETE`)
11. `ALERT_CATEGORIES` (`SELECT`)
12. `AUTOMATION_RULES` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
13. `RULE_ACTIONS` (`SELECT`, `INSERT`, `UPDATE`, `DELETE`)
14. `NOTIFICATION_PREFERENCES` (`SELECT`, `INSERT`, `UPDATE`, `DELETE` for own `user_id`)
15. `DUAL` (`SELECT`)

### Blocked USER Tables (Identity & Authorization Security)
1. `USERS` — Blocked to prevent privilege escalation or credential tampering.
2. `USER_CONTACT_NUMBERS` — Blocked to protect user identity and contact data.
3. `HOME_ACCESS` — Blocked to prevent unauthorized home access grants.
4. Direct `INSERT INTO HOMES` — Blocked to prevent unowned/orphan residences.

---

## 4. SQL Templates Dashboard

The SQL Console in `HomeDetails.jsx` / `SqlConsole.jsx` provides structured templates organized across 5 categories:

### A. `[ SELECT ]` Templates
- **Devices in Home**: `SELECT device_id, device_name, device_subtype, status, room_id FROM devices`
- **Device Status & Uptime**: `SELECT device_name, device_subtype, status, last_restart_time FROM devices`
- **Rooms & Floor Layout**: `SELECT room_id, room_name, floor_number, home_id FROM rooms ORDER BY floor_number, room_name`
- **Active Alerts**: `SELECT alert_id, device_id, message, alert_time, status FROM alerts WHERE status = 'ACTIVE' ORDER BY alert_time DESC`
- **Recent Sensor Readings**: `SELECT device_id, reading_id, value, reading_time FROM sensor_readings ORDER BY reading_time DESC FETCH FIRST 20 ROWS ONLY`
- **Thermostat Modes**: `SELECT d.device_id, d.device_name, t.target_temperature, t."MODE" FROM devices d JOIN thermostats t ON d.device_id = t.device_id`
- **Smart Light Brightness**: `SELECT d.device_id, d.device_name, l.brightness, l.color_support FROM devices d JOIN smart_lights l ON d.device_id = l.device_id`
- **Parent/Child Hierarchy**: `SELECT child.device_id, child.device_name, child.device_subtype, parent.device_name AS controller FROM devices child LEFT JOIN devices parent ON child.parent_device_id = parent.device_id`
- **Automation Rules & Actions**: `SELECT r.rule_id, r.rule_name, r.condition_operator, r.condition_value, a.action_type, a.action_value FROM automation_rules r LEFT JOIN rule_actions a ON r.rule_id = a.rule_id`
- **Accessible Homes**: `SELECT home_id, home_name, street, city, pincode FROM homes`

### B. `[ INSERT ]` Templates
- **Add Room**: `INSERT INTO rooms (room_id, room_name, floor_number, home_id) VALUES (seq_room_id.NEXTVAL, 'Guest Suite', 1, ${homeId})`
- **Add Device (Base)**: `INSERT INTO devices (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id) VALUES (seq_device_id.NEXTVAL, 'Balcony Light', 'SMART_LIGHT', 'ONLINE', SYSDATE, SYSTIMESTAMP, 1, NULL)`
- **Add Smart Light Subtype**: `INSERT INTO smart_lights (device_id, brightness, color_support) VALUES (1, 80.00, 'YES')`
- **Add Thermostat Subtype**: `INSERT INTO thermostats (device_id, target_temperature, "MODE") VALUES (1, 22.50, 'HEAT')`
- **Record Sensor Reading**: `INSERT INTO sensor_readings (device_id, reading_id, value, reading_time) VALUES (1, seq_reading_id.NEXTVAL, 23.40, SYSTIMESTAMP)`
- **Create Automation Rule**: `INSERT INTO automation_rules (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date) VALUES (seq_rule_id.NEXTVAL, 'Night Temp Regulation', '>', '26.0', 1, ${userId}, SYSDATE)`
- **Create Rule Action**: `INSERT INTO rule_actions (action_id, rule_id, target_device_id, action_type, action_value) VALUES (seq_action_id.NEXTVAL, 1, 1, 'SET_TEMP', '21.0')`
- **Notification Preference**: `INSERT INTO notification_preferences (user_id, device_id, category_id, channel, enabled) VALUES (${userId}, 1, 1, 'EMAIL', 1)`

### C. `[ UPDATE ]` Templates
- **Set Device Offline**: `UPDATE devices SET status = 'OFFLINE' WHERE device_id = 1`
- **Rename Device**: `UPDATE devices SET device_name = 'Master Bedroom Pendant' WHERE device_id = 1`
- **Update Light Brightness**: `UPDATE smart_lights SET brightness = 75.00 WHERE device_id = 1`
- **Update Thermostat Target**: `UPDATE thermostats SET target_temperature = 21.00, "MODE" = 'COOL' WHERE device_id = 1`
- **Rename Room**: `UPDATE rooms SET room_name = 'Executive Study' WHERE room_id = 1`
- **Acknowledge Alert**: `UPDATE alerts SET status = 'ACKNOWLEDGED', acknowledged_by = ${userId}, acknowledged_timestamp = SYSTIMESTAMP WHERE alert_id = 1`
- **Disable Notification Pref**: `UPDATE notification_preferences SET enabled = 0 WHERE user_id = ${userId} AND device_id = 1 AND category_id = 1`

### D. `[ DELETE ]` Templates
- **Delete Alert**: `DELETE FROM alerts WHERE alert_id = 999`
- **Delete Rule Action**: `DELETE FROM rule_actions WHERE action_id = 999`
- **Delete Notification Pref**: `DELETE FROM notification_preferences WHERE user_id = ${userId} AND device_id = 1 AND category_id = 1`
- **Delete Sensor Reading**: `DELETE FROM sensor_readings WHERE device_id = 1 AND reading_id = 999`
- **Delete Light Subtype**: `DELETE FROM smart_lights WHERE device_id = 999`
- **Delete Device**: `DELETE FROM devices WHERE device_id = 999`
- **Delete Room**: `DELETE FROM rooms WHERE room_id = 999`

### E. `[ PL/SQL ]` Dashboard
- **FUNCTIONS**:
  - `FN_CALCULATE_DEVICE_UPTIME` — Computes derived device uptime string from last restart/install timestamp.
  - `FN_CHECK_DEVICE_ACCESS` — Evaluates M:N authorization for authenticated user.
- **PROCEDURES**:
  - `SP_ACKNOWLEDGE_ALERT` — Transitions alert status to `ACKNOWLEDGED` with user ID audit logging.
  - `SP_HOME_DEVICE_HEALTH_REPORT` — Executes explicit cursor procedure and streams `DBMS_OUTPUT` diagnostic report.
  - `SP_RECORD_SENSOR_READING` — Inserts weak entity reading using `SEQ_READING_ID` sequence with `OUT` parameter capture.
- **TRIGGERS**:
  - `TEST_TRIGGER_CYCLE` — Performs controlled self-parent circular reference test to verify `TRG_PREVENT_DEVICE_CYCLE` triggers `ORA-20030`.

---

## 5. Backend Endpoints & Architecture

Two secure REST endpoints in [`SqlConsoleController.java`](file:///c:/Users/akhil/OneDrive/Desktop/Projects/dbms/backend/src/main/java/com/smarthome/smart_home_backend/controller/SqlConsoleController.java) serve the SQL Console:

1. `POST /api/sql-console/execute`:
   - Handles `SELECT`, `INSERT`, `UPDATE`, `DELETE`.
   - Admin: Executes directly within transaction.
   - User: Scopes `SELECT` via table rewrites, pre-validates `INSERT` parent relationships, and injects `WHERE` scope predicates for `UPDATE` / `DELETE`.
2. `POST /api/sql-console/execute-plsql`:
   - Dedicated allowlist handler for approved PL/SQL routines.
   - Arbitrary anonymous PL/SQL (`BEGIN`, `DECLARE`, `EXEC`) remains completely blocked.
   - Authenticated user ID is derived directly from the Spring Security context, never trusted from the client payload.

---

## 6. Automated Test Results (`Phase9SqlConsoleTest.java`)

All 15 automated test cases executed against the live Oracle 21c XE database and passed with **0 errors**:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.smarthome.smart_home_backend.security.Phase9SqlConsoleTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.74 s -- in com.smarthome.smart_home_backend.security.Phase9SqlConsoleTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Key Test Case Verifications
1. `testAdminCanExecuteSelect`: Verified ADMIN can query devices across all homes.
2. `testAdminDdlIsProhibited`: Verified DDL `DROP TABLE` is blocked for all roles.
3. `testMultipleStatementsRejected`: Verified semicolon query stacking is rejected.
4. `testUserSelectIsScopedToAccessibleHome`: Verified USER only receives rows from Home 1.
5. `testUserCannotQueryIdentityTables`: Verified USER cannot `SELECT` from `USERS`.
6. `testUserCannotMutateIdentityTables`: Verified USER cannot `UPDATE` `USERS`.
7. `testUserDirectInsertIntoHomesIsProhibited`: Verified direct `INSERT INTO HOMES` is rejected.
8. `testUserAccessibleInsertAndUpdateAndRollback`: Verified USER can insert/update/delete in accessible Home 1 with clean rollback.
9. `testUserForeignInsertAndUpdateAreRejected`: Verified cross-home `INSERT` and `UPDATE` into Home 2 are rejected with `403 Access Denied`.
10. `testPlSqlFunctionUptime`: Verified `FN_CALCULATE_DEVICE_UPTIME` returns real formatted uptime string.
11. `testPlSqlFunctionCheckDeviceAccess`: Verified `FN_CHECK_DEVICE_ACCESS` returns `1 (TRUE)` for Home 1 devices and `0 (FALSE)` for foreign Home 2 devices.
12. `testPlSqlProcedureHealthReport`: Verified `SP_HOME_DEVICE_HEALTH_REPORT` captures and streams `DBMS_OUTPUT` diagnostic lines.
13. `testPlSqlProcedureRecordSensorReadingAndCleanup`: Verified `SP_RECORD_SENSOR_READING` captures sequence-generated `reading_id` and persists into `SENSOR_READINGS`.
14. `testPlSqlTriggerTestCyclePrevention`: Verified `TRG_PREVENT_DEVICE_CYCLE` triggers `ORA-20030` on self-parent assignment.
15. `testUnapprovedPlSqlRoutineRejected`: Verified unapproved routines (e.g. `DBMS_UTILITY.*`) are rejected.

---

## 7. Database Integrity & Row Count Verification

Exact row counts across all **17 Oracle tables** verified via SQL*Plus:

```sql
SELECT table_name, exact_count FROM ...
```

| Table Name | Baseline Rows | Current Rows | Status |
| :--- | :--- | :--- | :--- |
| `ALERTS` | 35 | 35 | Verified |
| `ALERT_CATEGORIES` | 5 | 5 | Verified |
| `AUTOMATION_RULES` | 15 | 15 | Verified |
| `CAMERAS` | 7 | 7 | Verified |
| `DEVICES` | 45 | 45 | Verified |
| `HOMES` | 5 | 5 | Verified |
| `HOME_ACCESS` | 14 | 14 | Verified |
| `MOTION_SENSORS` | 8 | 8 | Verified |
| `NOTIFICATION_PREFERENCES` | 25 | 25 | Verified |
| `ROOMS` | 23 | 23 | Verified |
| `RULE_ACTIONS` | 25 | 25 | Verified |
| `SENSOR_READINGS` | 120 | 120 | Verified |
| `SMART_LIGHTS` | 10 | 10 | Verified |
| `TEMPERATURE_SENSORS` | 10 | 10 | Verified |
| `THERMOSTATS` | 5 | 5 | Verified |
| `USERS` | 10 | 10 | Verified |
| `USER_CONTACT_NUMBERS` | 14 | 14 | Verified |
| **TOTAL** | **376** | **376** | **Exact Match (100% Clean Baseline)** |

*(Note: Baseline is 376 total rows across all 17 master tables: 48 original readings in `04_sample_data.sql` + 72 expanded readings in `04b_expand_sample_data.sql` = 120 sensor readings).*
