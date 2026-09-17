# Smart Home / IoT Device Monitoring System - Architecture & Schema Reference

**Database**: Oracle Database 21c Express Edition  
**PDB**: `XEPDB1`  
**Schema / User**: `SMART_HOME`  
**Implementation**: Full Enhanced Entity-Relationship (EER) Model (17 Tables)

---

## 1. Directory Structure

```
dbms_assignment/
│
├── backend/                  # Spring Boot 4.x / Java 21 REST API
│   ├── src/
│   ├── pom.xml
│   └── mvnw / mvnw.cmd
│
├── frontend/                 # React 19 / Vite / Tailwind CSS
│   ├── src/
│   ├── package.json
│   └── vite.config.js
│
├── database/                 # Oracle 21c SQL & PL/SQL Scripts
│   ├── 01_create_tables.sql  # 17 tables in strict dependency order
│   ├── 02_constraints.sql    # Foreign keys, unique, and check constraints
│   ├── 03_sequences.sql      # Surrogate key sequences
│   ├── 04_sample_data.sql    # Realistic multi-home/device test data
│   ├── 05_queries.sql        # Analytical, authorization, & aggregate queries
│   └── 06_plsql.sql          # Stored procedures, functions, cursors, triggers
│
├── docs/                     # Project architectural documentation
│   └── architecture.md
│
├── .gitignore                # Unified monorepo gitignore
└── README.md                 # Project overview and run guides
```

---

## 2. Core EER Architectural Decisions

1. **M:N User–Home Relationship via `HOME_ACCESS`**:
   - `USERS` does not have a `home_id`.
   - `HOMES` does not have a `user_id`.
   - All access control is resolved through `HOME_ACCESS` with roles (`OWNER`, `MEMBER`, etc.) and `date_granted`.

2. **Unary / Recursive Device Controls**:
   - `DEVICES.parent_device_id` models a hub device controlling subordinate IoT sensors/actuators.
   - Self-referencing FK is defined via `ALTER TABLE` to avoid initialization order dependencies.
   - Cycle prevention is enforced via database constraint and trigger (`trg_prevent_device_cycle`).

3. **Weak Entity `SENSOR_READINGS`**:
   - Dependent on `DEVICES`.
   - Composite Primary Key: `(device_id, reading_id)`.
   - Identifying relationship with cascade deletion.

4. **Multi-valued Attribute Resolution via `USER_CONTACT_NUMBERS`**:
   - Stores multiple telephone numbers per user with `number_type` (e.g. `'MOBILE'`, `'HOME'`).
   - Composite Primary Key: `(user_id, contact_number)`.

5. **Ternary Relationship `NOTIFICATION_PREFERENCES`**:
   - Associates `(user_id, device_id, category_id)` with `channel` and `enabled` flag (`CHECK (enabled IN (0, 1))`).
   - Genuinely irreducible to binary pairs.

6. **Derived Uptime**:
   - Uptime is computed on-the-fly (`SYSTIMESTAMP - last_restart_time`, fallback `install_date`).
   - No persistent `uptime` column in the database table.
