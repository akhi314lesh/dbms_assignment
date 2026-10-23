# Change Safety Model & Architectural Invariants

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Blast radius control, non-negotiable invariants, and change evaluation checklist  

---

## 1. Action Safety Classifications

Every command, edit, or script executed by an agent or engineer falls into one of these four risk tiers:

| Tier | Classification | Definition | Examples | Agent Policy |
| :-: | :--- | :--- | :--- | :--- |
| 🟢 | **SAFE** | Idempotent, read-only, or non-destructive runtime reloads. | `docker stats`, `curl`, `nginx -s reload`, `demo.ps1` | Execute freely without confirmation. |
| 🟡 | **CAUTION** | Modifies non-persistent runtime state or restarts services. | `docker restart`, config edits, rebuilding container | Verify preconditions; observe post-action logs. |
| 🟠 | **DESTRUCTIVE** | Modifies schema, wipes local cache, or deletes state. | `docker compose down -v`, schema DDL, deleting files | Require explicit planning and rollback plan. |
| 🔴 | **IRREVERSIBLE**| Permanent loss of data or committed history. | `DROP TABLE`, `git push --force`, deleting secret files | **STRICTLY PROHIBITED** without human approval. |

---

## 2. The 10-Question Change Safety Checklist

Before modifying any file in this repository, answer these 10 questions:

1. **Subsystem Impact:** Which of the 6 subsystems does this change affect (Backend, Frontend, Database, Nginx, Docs, Scripts)?
2. **Contract Surface:** Does this alter any REST endpoint, DTO schema, database column, or Nginx directive?
3. **Dependency Drift:** Does this introduce a new dependency, library, or port requirement?
4. **Invalid State Risk:** Could this leave the container image or database in an unbootable state?
5. **Blast Radius:** If this change fails, what breaks (single replica, load balancer, or entire cluster)?
6. **Minimal Delta:** Is this the smallest possible change that achieves the objective?
7. **Cheapest Verification:** What is the fastest command to verify success without a full system rebuild?
8. **Rollback Mechanism:** How do I undo this change in under 30 seconds if verification fails?
9. **Documentation Delta:** Which canonical document in `docs/` must be updated to prevent drift?
10. **Evidence Preservation:** What command output or log proves that the change succeeded?

---

## 3. Extensibility Contracts

When adding new features or capabilities:

1. **Adding a New REST Endpoint:**
   - Place controller in `com.smarthome.smart_home_backend.controller`.
   - If public diagnostic endpoint: register in `SecurityConfig.java` `permitAll()`.
   - If secure business endpoint: annotate with `@PreAuthorize` and verify user tenancy through `HomeAuthorizationService`.
2. **Adding a New Database Table:**
   - Must be numbered sequentially in `database/` (e.g. `08_new_feature.sql`).
   - Must declare explicit primary keys, foreign keys, and audit timestamps.
   - Must not alter existing 17 core tables without an ADR.
3. **Scaling to 3+ Backend Replicas:**
   - Add `backend3` to `docker-compose.yml` with `INSTANCE_NAME=backend-3`.
   - Add `server backend3:8080;` to `nginx/nginx.conf` upstream block.
   - Reload Nginx: `docker exec smart-home-nginx nginx -s reload`. Zero application code changes required.
