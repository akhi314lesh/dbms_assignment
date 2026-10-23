# ADR-004: In-Browser Spatial Digital Twin Synthesis via Three.js

**Status:** Accepted & Active  
**Date:** September 2026  
**Context:** Need for a real-time, interactive 3D representation of estate layouts and device states without heavy game-engine runtimes.  
**Decision:** Implement `frontend/visual.html` using Three.js (WebGL) to dynamically parse room dimensions, device coordinates, and operational statuses (`ONLINE`, `OFFLINE`, `ERROR`) directly from live Spring Boot REST endpoints.  
**Consequences:**
- **Positive:** Zero server-side rendering burden; runs natively in any standard modern browser; live visual feedback with emissive status glows.
- **Trade-off:** Client requires WebGL GPU acceleration.
**Reversibility:** High. Standalone HTML/JS module decoupled from core React components.
