# System Invariants & Architectural Axioms

This document records the foundational axioms that must never be violated by any agent turn:

1. **Schema Authority:** Oracle 21c XE relational schema is canonical. Hibernate DDL generation is strictly disabled (`spring.jpa.hibernate.ddl-auto=none`).
2. **Stateless Session Rule:** No HTTP sessions are ever created on the backend. `SessionCreationPolicy.STATELESS` is mandatory.
3. **Container Symmetry:** Backend containers must always run identical images and configurations, differing only in `INSTANCE_NAME`.
4. **Secret Isolation:** Passwords (`DB_PASSWORD`), Firebase service-account JSON keys, and client tokens must never be committed to Git.
5. **Port Discipline:** Host port `8000` is reserved for Nginx, `1521` for Oracle XE, `5173` for Vite frontend. Port `8080` is internal to containers or reserved for host local dev.
6. **Zero Circular Device Hierarchies:** Enforced by database trigger `TRG_PREVENT_DEVICE_CYCLE` (`ORA-20030`).
