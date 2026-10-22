# AGENTS.md — Agent Operating Contract & System Manifesto

**Target Audience:** Autonomous AI Agents, Pair-Programming Assistants, and Systems Engineers  
**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Primary onboarding, cognitive orientation, operating loop, and safety invariants  

---

## ⚡ Fast Path: First 10 Minutes in the Repository

If you are an agent entering this repository for the first time, execute this sequence before touching code:

1. **Orient (30 seconds):**
   - Read this document (`AGENTS.md`).
   - Identify the three running tiers: **React 19 Frontend** (`:5173`), **Spring Boot 4.1.1 Dual Backends** (`:8080`), and **Nginx Load Balancer** (`:8000`), connecting to **Host Oracle Database 21c XE** (`:1521/XEPDB1`).
2. **Observe Current State (1 minute):**
   ```powershell
   # Check active Docker containers
   docker compose ps
   # Check if host Oracle Database is listening on port 1521
   netstat -ano | findstr :1521
   ```
3. **Verify API & Health (1 minute):**
   ```powershell
   # Hit load-balanced instance endpoint through Nginx
   curl.exe -s http://localhost:8000/api/instance
   # Run full 10-second automated diagnostic dashboard
   .\scripts\demo.ps1
   ```
4. **Locate the Canonical Documentation Graph:**
   - Architecture & Tower of Abstractions: [`docs/SYSTEM_ARCHITECTURE.md`](docs/SYSTEM_ARCHITECTURE.md)
   - State Taxonomy & Health Checks: [`docs/SYSTEM_STATE_MODEL.md`](docs/SYSTEM_STATE_MODEL.md)
   - Capability Cards: [`docs/CAPABILITIES.md`](docs/CAPABILITIES.md)
   - Runbooks & Control Surfaces: [`docs/OPERATIONS.md`](docs/OPERATIONS.md)
   - Failure Catalog & Recovery: [`docs/FAILURE_AND_RECOVERY.md`](docs/FAILURE_AND_RECOVERY.md)
   - Change Safety & Invariants: [`docs/CHANGE_SAFETY.md`](docs/CHANGE_SAFETY.md)
   - Repository Map & Ownership: [`docs/REPOSITORY_MAP.md`](docs/REPOSITORY_MAP.md)

---

## 🧭 The Canonical Agent Operating Loop

Every action performed by an agent in this repository must follow this 9-stage disciplined lifecycle:

```text
+----------+      +----------+      +--------+      +------------+      +-------+
| OBSERVE  | ---> |  ORIENT  | ---> |  PLAN  | ---> |  PRECHECK  | ---> |  ACT  |
+----------+      +----------+      +--------+      +------------+      +-------+
                                                                            |
+----------------------+      +--------------+      +----------+            |
| ACCUMULATE KNOWLEDGE | <--- | UPDATE STATE | <--- |  RECORD  | <--- VERIFY+
+----------------------+      +--------------+      +----------+
```

1. **OBSERVE:** Gather empirical facts from the environment using lowest-cost commands (`docker compose ps`, `netstat`, health endpoints). Never guess state.
2. **ORIENT:** Map observations to [`docs/SYSTEM_STATE_MODEL.md`](docs/SYSTEM_STATE_MODEL.md). Determine whether the system is healthy, degraded, or failed.
3. **PLAN:** Select the smallest, lowest-cost action that satisfies the user objective. Review [`docs/RESOURCE_AWARE_OPERATIONS.md`](docs/RESOURCE_AWARE_OPERATIONS.md).
4. **PRECHECK:** Validate architectural invariants against [`docs/CHANGE_SAFETY.md`](docs/CHANGE_SAFETY.md). Confirm that ports, passwords, schema, and contracts will not be violated.
5. **ACT:** Execute the action cleanly (file edit, test run, or config reload).
6. **VERIFY:** Execute the designated verification test from [`docs/VALIDATION_MATRIX.md`](docs/VALIDATION_MATRIX.md). Confirm both positive results and absence of regressions.
7. **RECORD:** Collect deterministic evidence (logs, HTTP responses, metrics). Distinguish **Claim** from **Observation** and **Verification**.
8. **UPDATE STATE:** If component topology or config changed, update mutable state documentation.
9. **ACCUMULATE KNOWLEDGE:** Promote new discoveries, failure signatures, or architectural choices into durable knowledge files in [`docs/KNOWLEDGE/`](docs/KNOWLEDGE/) or [`docs/DECISIONS/`](docs/DECISIONS/).

---

## 🛡️ Invariants & Non-Negotiable Guarantees

Before making any change, verify that none of these 8 system invariants will be broken:

1. **Oracle Schema Authority:** The Oracle 21c XE relational schema is the single source of truth (`ddl-auto=none`). Hibernate does not alter tables.
2. **Database Placement:** Oracle Database runs on the Windows host on port `1521` (`XEPDB1`). It is **never** containerized or deleted.
3. **Stateless Backend Auth:** Spring Boot backends use stateless JWT validation via Firebase Admin SDK (`SessionCreationPolicy.STATELESS`). Backends hold zero HTTP session state.
4. **Identical Backend Replicas:** `smart-home-backend-1` and `smart-home-backend-2` run the exact same container image (`smart-home-backend:latest`).
5. **Port Isolation:** Backends listen on internal port `8080`. Only Nginx (`smart-home-nginx`) exposes a public ingress port (`8000:80`).
6. **Zero Secrets in Git:** Database passwords (`DB_PASSWORD`), Firebase service-account JSON keys, and user JWTs are never committed to version control.
7. **Local Host Runnability:** The local development setup (`mvnw spring-boot:run` on `:8080` + Vite dev on `:5173`) must remain 100% operational outside Docker.
8. **CORS Boundary:** Spring Boot handles CORS headers. Nginx forwards `Origin` and client headers without adding duplicate `Access-Control-*` headers.

---

## 🗂️ Documentation Graph & Navigation

```text
[ AGENTS.md ] (You are here - Operating Contract & Fast Path)
      |
      +---> [ README.md ] (Project Overview & Human Presentation)
      |
      +---> [ docs/SYSTEM_ARCHITECTURE.md ] (Tower of Abstractions L0-L10 & Topologies)
      |         |
      |         +---> [ docs/SYSTEM_STATE_MODEL.md ] (State Taxonomy & Health Evaluation)
      |         +---> [ docs/CAPABILITIES.md ] (Machine-Legible Capability Catalog)
      |         +---> [ docs/REPOSITORY_MAP.md ] (Directory & File Ownership Matrix)
      |
      +---> [ docs/OPERATIONS.md ] (Runbooks: Startup, Shutdown, Switchover, Recovery)
      |         |
      |         +---> [ docs/VALIDATION_MATRIX.md ] (Empirical Verification Tests)
      |         +---> [ docs/RESOURCE_AWARE_OPERATIONS.md ] (Cost-Tier Decision Principles)
      |
      +---> [ docs/FAILURE_AND_RECOVERY.md ] (Failure Signatures & Recovery Trees)
      |         |
      |         +---> [ docs/OBSERVABILITY_AND_EVIDENCE.md ] (Metrics, Logs & Evidence Hierarchy)
      |
      +---> [ docs/CHANGE_SAFETY.md ] (Blast Radius Checklist & Change Templates)
                |
                +---> [ docs/DECISIONS/ ] (Architectural Decision Records ADR-001 to ADR-004)
                +---> [ docs/KNOWLEDGE/ ] (Durable Facts, Invariants & Network Maps)
```

---

## 🎯 Answering the 14 Core Agent Questions

| # | Question | Canonical Reference |
| :-: | :--- | :--- |
| **1** | What is this system? | [`docs/SYSTEM_ARCHITECTURE.md`](docs/SYSTEM_ARCHITECTURE.md#1-system-purpose--identity) |
| **2** | What are its major abstractions? | [`docs/SYSTEM_ARCHITECTURE.md`](docs/SYSTEM_ARCHITECTURE.md#2-the-tower-of-abstractions-layer-0-to-layer-10) |
| **3** | What is the current state? | [`docs/SYSTEM_STATE_MODEL.md`](docs/SYSTEM_STATE_MODEL.md) (Run `docker compose ps` & `.\scripts\demo.ps1`) |
| **4** | What depends on what? | [`docs/SYSTEM_ARCHITECTURE.md`](docs/SYSTEM_ARCHITECTURE.md#4-dependency-graph) |
| **5** | What can I safely change? | [`docs/CHANGE_SAFETY.md`](docs/CHANGE_SAFETY.md#safe-actions) |
| **6** | What should I never change casually? | [`docs/CHANGE_SAFETY.md`](docs/CHANGE_SAFETY.md#caution--destructive-actions) |
| **7** | What actions can I perform? | [`docs/CAPABILITIES.md`](docs/CAPABILITIES.md) & [`docs/OPERATIONS.md`](docs/OPERATIONS.md) |
| **8** | What is the cheapest safe next action? | [`docs/RESOURCE_AWARE_OPERATIONS.md`](docs/RESOURCE_AWARE_OPERATIONS.md#cost-tier-decision-matrix) |
| **9** | How do I verify whether an action worked? | [`docs/VALIDATION_MATRIX.md`](docs/VALIDATION_MATRIX.md) |
| **10**| How do I detect partial failure? | [`docs/FAILURE_AND_RECOVERY.md`](docs/FAILURE_AND_RECOVERY.md#detection-matrix) |
| **11**| How do I recover? | [`docs/FAILURE_AND_RECOVERY.md`](docs/FAILURE_AND_RECOVERY.md#recovery-procedures) |
| **12**| What evidence proves the current state? | [`docs/OBSERVABILITY_AND_EVIDENCE.md`](docs/OBSERVABILITY_AND_EVIDENCE.md#evidence-hierarchy) |
| **13**| What info should be persisted for future agents?| [`docs/KNOWLEDGE/`](docs/KNOWLEDGE/) & [`docs/DECISIONS/`](docs/DECISIONS/) |
| **14**| How can the system grow without becoming harder to understand?| [`docs/CHANGE_SAFETY.md`](docs/CHANGE_SAFETY.md#extensibility-contracts) |
