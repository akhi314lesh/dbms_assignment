# Phase 10 — Deployment Documentation Report

**Project:** Smart Home Management System  
**Date:** October 2026  
**Status:** Phase 10 Finalized (Documentation & Deployment Architecture Complete)  

---

## 1. Executive Summary

Phase 10 finalizes the deployment architecture and documentation for the Smart Home Management System. The project architecture supports full local development, a temporary public demonstration topology utilizing Vercel and Cloudflare Quick Tunnels, and a documented path towards a permanent production cloud deployment.

---

## 2. System Architecture & Topology

### 2.1 Component Overview

```text
[ React 19 Frontend (Vite) ]
          |
          v
    [ Vercel CDN ]
          |
          v (HTTPS)
[ Cloudflare Quick Tunnel ]
          |
          v
[ Spring Boot 3.4 REST Backend (localhost:8080) ]
          |
          v (JDBC / HikariCP)
[ Oracle Database 21c XE (localhost:1521/XEPDB1) ]
```

### 2.2 Authentication Provider
- **Provider:** Firebase Authentication (Google Sign-In & Email/Password).
- **Verification:** Client sends Firebase JWT Bearer tokens to backend `/api/*` endpoints. Spring Boot's `FirebaseAuthenticationFilter` verifies tokens via the Firebase Admin SDK and provisions/resolves local user identities in Oracle Database (`USERS` table).

---

## 3. Current Deployment Status Breakdown

| Layer / Component | Status | Empirical Verification Detail |
| :--- | :--- | :--- |
| **GitHub Repository** | **Completed** | Repository structure configured with `backend/`, `frontend/`, `database/`, `docs/`, `.gitignore`, and `README.md`. |
| **Backend & Local Database** | **Verified** | Spring Boot 3.4 API runs on `localhost:8080` connected to Oracle XE 21c (`localhost:1521/XEPDB1`). |
| **Public Backend Tunnel** | **Verified** | Cloudflare Quick Tunnel successfully verified via Newman/REST automated testing: <br>• `ADMIN GET /api/homes` -> `200 OK` (all 5 homes)<br>• `USER GET /api/homes` -> `200 OK` (only authorized Home 1)<br>• `USER GET /api/homes/2` -> `403 Forbidden` (security scoping enforced) |
| **Frontend Production Build** | **Verified** | Centralized API URL management (`VITE_API_URL`). `npm run build` succeeds cleanly with 0 errors. |
| **Vercel Deployment** | **Created (Ready)** | Frontend static application successfully deployed and reached Ready status on Vercel. |
| **End-to-End Integration** | **Unverified** | Complete end-to-end browser integration (Vercel CDN -> Cloudflare Quick Tunnel -> Local Backend -> Oracle XE) has **not** been verified in live browser runtime. |

---

## 4. Temporary Quick Tunnel Architecture

The public backend endpoint was made available temporarily using Cloudflare Quick Tunnel:

```bash
cloudflared tunnel --url http://localhost:8080
```

### ⚠️ Temporary Nature & Limitations:
1. **Dynamic URLs:** Quick Tunnels generate ephemeral hostnames (e.g. `https://dover-hitachi-base-pubmed.trycloudflare.com`). Recreating the tunnel generates a new URL.
2. **Process Dependency:** The `cloudflared` process, local Spring Boot server, and local Oracle XE database must remain active continuously.
3. **Re-deployment Requirement:** Whenever the tunnel URL changes, the frontend build environment variable `VITE_API_URL` must be updated and Vercel redeployed.
4. **Non-Production Rating:** This setup is designed exclusively for developer testing, demonstration, and experimentation.

---

## 5. Future Permanent Deployment Path

To transition from temporary public access to a permanent production environment:

### Option A — Named Persistent Cloudflare Tunnel
- Register a named Cloudflare Tunnel bound to a fixed domain hostname (e.g. `api.smarthome.example.com`).
- Eliminates URL instability while allowing local Spring Boot + Oracle XE hosting.

### Option B — Fully Cloud-Hosted Infrastructure (Recommended)
- Host Spring Boot on a cloud platform (e.g., Railway, Render, or AWS).
- Connect to a managed cloud database.
- Point Vercel `VITE_API_URL` directly to the permanent cloud backend URL.

### Database Hosting Note:
Oracle XE 21c was retained locally because Oracle Autonomous AI Database on OCI required payment-card verification during sign-up. Migration to cloud database hosting remains a future milestone.

---

## 6. Security Assurance & Best Practices

- **Zero Secrets in Git:** Database passwords, Firebase Admin service accounts (`.json`), and JWT tokens are strictly kept out of version control.
- **Client Bundle Safety:** `VITE_*` environment variables contain client-safe identifiers only.
- **Network Isolation:** Oracle XE port `1521` is never directly exposed to the internet; traffic is safely proxied through Spring Boot REST APIs and encrypted tunnels.
