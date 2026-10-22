# Network Topology, Gateways & Port Directory

| Entity | Location | Port | Protocol | Access Boundary | Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **smart-home-nginx** | Docker Container | `8000` (host) -> `80` (container) | HTTP | Public Host Ingress | Primary entry point & load balancer |
| **smart-home-backend-1**| Docker Container | `8080` (container only) | HTTP | Internal Docker Net (`smart-home-net`) | Primary application replica 1 |
| **smart-home-backend-2**| Docker Container | `8080` (container only) | HTTP | Internal Docker Net (`smart-home-net`) | Identical application replica 2 |
| **Oracle Database 21c** | Windows Host | `1521` | TCP / T4C | Host / Docker Gateway | Primary relational database (`XEPDB1`) |
| **React / Vite Dev** | Windows Host | `5173` | HTTP | Host Browser | Frontend Single Page Application |
| **Docker Host Gateway** | Bridge Network | `host.docker.internal` | DNS / Routing | Container -> Host | Allows containers to reach host port 1521 |
