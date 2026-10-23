# ADR-001: Retention of Oracle Database 21c Express Edition on Windows Host

**Status:** Accepted & Active  
**Date:** September 2026  
**Context:** Migration to Oracle Autonomous AI Database on Oracle Cloud Infrastructure (OCI) was evaluated for production deployment. However, OCI registration required payment-card verification that could not be completed by the developer.  
**Decision:** Retain the development and demonstration baseline on Oracle Database 21c Express Edition running directly on the Windows host machine (port 1521, PDB `XEPDB1`). When containerizing backend applications with Docker, connect to the host Oracle instance via `host.docker.internal:1521/XEPDB1`.  
**Consequences:**
- **Positive:** Zero cloud cost, 100% offline development, existing 17 normalized tables and PL/SQL procedures function without migration risk.
- **Trade-off:** The developer machine must remain running during demonstration, and containers rely on Docker host gateway networking.
**Reversibility:** High. Cloud migration requires only updating JDBC URL, credentials, and wallet file in Spring Boot properties.
