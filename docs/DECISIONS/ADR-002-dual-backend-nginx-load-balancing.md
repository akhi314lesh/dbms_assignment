# ADR-002: Dual-Backend Container Architecture Fronted by Nginx Reverse Proxy

**Status:** Accepted & Active  
**Date:** October 2026  
**Context:** College assignment required demonstrating multi-container deployment, load balancing (round-robin and weighted), resource monitoring, and container identification.  
**Decision:** 
1. Build a single lightweight Docker image (`smart-home-backend:latest`) using Eclipse Temurin 21 JRE Alpine.
2. Deploy two identical container instances (`smart-home-backend-1` and `smart-home-backend-2`) differentiated only by the runtime environment variable `INSTANCE_NAME` (`backend-1` and `backend-2`).
3. Deploy an Nginx Alpine container (`smart-home-nginx`) as the sole public ingress point on host port `8000`, proxying traffic to upstream `backend_cluster`.
4. Keep backend containers isolated on internal bridge network `smart-home-net`.
**Consequences:**
- **Positive:** Proves true horizontal scalability and load distribution; zero code differences between replicas.
- **Trade-off:** Requires Docker Desktop running on host; memory footprint increases by ~380MB per container.
**Reversibility:** High. Stack can be stopped with `docker compose down` and run directly on host.
