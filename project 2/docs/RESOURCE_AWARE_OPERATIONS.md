# Resource-Aware Operations & Efficiency Decision Principles

**System:** Smart Home / IoT Device Monitoring System  
**Canonical Role:** Decision principles to prevent expensive, wasteful, or destructive operations  

---

## 1. The Core Efficiency Principle

> **"Always use the least expensive action that can confidently distinguish the current hypothesis."**

Never execute a 60-second full container rebuild when a 50-millisecond curl probe or 1-second configuration reload can answer the question.

---

## 2. Operational Cost Tiers

```text
[ TIER 1: ULTRA-LOW COST (1ms - 50ms) ]
  • curl.exe -s http://localhost:8000/api/instance
  • netstat -ano | findstr :1521
  • docker compose ps

[ TIER 2: LOW COST (100ms - 1s) ]
  • docker exec smart-home-nginx nginx -s reload  (Zero-downtime config switch)
  • docker logs --tail 25 smart-home-nginx
  • docker stats --no-stream
  • .\scripts	est-round-robin.ps1

[ TIER 3: MEDIUM COST (2s - 15s) ]
  • .\scripts\demo.ps1 (Full 7-point validation suite)
  • .\scripts	est-weighted.ps1 (40 requests + config switch + restore)
  • docker restart smart-home-backend-1

[ TIER 4: HIGH COST (20s - 90s) ]
  • .\mvnw.cmd package -DskipTests (Full Java compilation & JAR repackage)
  • docker build -t smart-home-backend:latest ./backend
  • docker compose up -d --force-recreate

[ TIER 5: PROHIBITED CASUAL ACTIONS ]
  • docker compose down -v (Wipes named volumes)
  • Deleting target/ or node_modules/ without verified corruption
  • Re-running database DDL scripts against populated Oracle schema
```

---

## 3. Rebuild Avoidance Heuristics

An agent must consult this table before initiating any rebuild or container restart:

| Proposed Action | Is Rebuild Necessary? | Preferred Low-Cost Alternative |
| :--- | :-: | :--- |
| **Change load balancer routing / weights** | **NO** | Edit/copy `nginx.conf` and run `docker exec smart-home-nginx nginx -s reload` (100ms). |
| **Verify container health** | **NO** | Probe `http://localhost:8000/api/instance` (5ms). |
| **Check database connection status** | **NO** | Inspect `docker logs smart-home-backend-1` for HikariPool lines (50ms). |
| **Test frontend API communication** | **NO** | Send curl request to Nginx entry point (5ms). |
| **Change backend Java business logic** | **YES** | Run `.\mvnw.cmd package -DskipTests`, then `docker build`, then `docker compose up -d`. |
| **Update frontend React component** | **NO** | Vite HMR updates instantly in dev mode (`npm run dev`). Zero rebuild needed. |
