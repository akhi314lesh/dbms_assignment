# ADR-003: Stateless Authentication via Firebase Admin SDK and Bearer JWTs

**Status:** Accepted & Active  
**Date:** September 2026  
**Context:** Multi-container load balancing requires either sticky sessions, shared Redis session storage, or stateless tokens.  
**Decision:** Implement strictly stateless JWT authentication (`SessionCreationPolicy.STATELESS` in Spring Security). Clients acquire JWTs via Firebase Web Client SDK and send `Authorization: Bearer <idToken>`. Each backend container independently validates tokens using the Google Firebase Admin SDK.  
**Consequences:**
- **Positive:** Any request can be routed to any backend container without session loss or sticky session configuration; perfectly suited for round-robin and weighted balancing.
- **Trade-off:** Firebase ID tokens expire after 1 hour and must be refreshed by the client; service account JSON must be mounted into containers.
**Reversibility:** Low. Stateless authentication is fundamental to the platform architecture.
