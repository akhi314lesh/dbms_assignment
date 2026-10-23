# Docker + Nginx Load Balancing — 5-8 Minute Video Demonstration Script

**Target Duration:** 5 to 8 minutes  
**Tone:** Confident, clear, student-level technical language  
**Prerequisites to have open:**
1. **Docker Desktop** window showing Containers and Images tabs.
2. **Terminal (PowerShell)** opened to project root `C:\Projects\dbms`.
3. **Web Browser** opened with two tabs:
   - `http://localhost:8000/api/instance`
   - `http://localhost:5173` (Frontend)

---

## Video Outline & Timeline

| Time | Segment | What to Show on Screen | What to Say |
| :--- | :--- | :--- | :--- |
| **0:00 - 0:45** | **Introduction & Architecture** | Project code in IDE, Architecture Diagram | *"Hello everyone. Today I am demonstrating a complete multi-container Docker deployment with Nginx load balancing for our Smart Home IoT project. In this setup, we have two identical Spring Boot backend containers running in Docker, fronted by an Nginx reverse proxy load balancer on port 8000, connecting to our existing Oracle Database running on the host machine."* |
| **0:45 - 1:30** | **Docker Environment Verification** | Terminal | Run `docker --version` and `docker compose version`.<br>*"First, let's verify our Docker environment. As you can see, Docker Desktop is running with Docker version 29 and Docker Compose v5."* |
| **1:30 - 2:30** | **Code & Config Overview** | IDE showing `Dockerfile`, `nginx.conf`, `docker-compose.yml` | *"Let's look at the configuration files. In `backend/Dockerfile`, we use a lightweight Eclipse Temurin Java 21 runtime and package our Spring Boot application. In `nginx/nginx.conf`, we define an upstream block called `backend_cluster` containing `backend1:8080` and `backend2:8080`. In `docker-compose.yml`, we configure both backend containers using the environment variable `INSTANCE_NAME` so each container can identify itself."* |
| **2:30 - 3:30** | **Container Status & Docker Desktop** | Docker Desktop GUI & Terminal `docker ps` | Run `docker compose ps` or show Docker Desktop.<br>*"Here in Docker Desktop, you can see our three active containers: `smart-home-nginx` listening on host port 8000, `smart-home-backend-1`, and `smart-home-backend-2`. Notice both backends are isolated inside the Docker network, and Nginx acts as our single public gateway."* |
| **3:30 - 5:00** | **Demonstrating Round-Robin Load Balancing** | Browser & Terminal | Open `http://localhost:8000/api/instance` and refresh.<br>Run in Terminal:<br>`python -c "import urllib.request, json; print(' -> '.join([json.loads(urllib.request.urlopen('http://localhost:8000/api/instance').read().decode())['instance'] for _ in range(10)]))"`<br>*"Now let's test load balancing. When we send requests to `http://localhost:8000/api/instance`, Nginx distributes them in a round-robin order. Notice how the response alternates between `backend-1` and `backend-2`. Each request is handled by a different container instance."* |
| **5:00 - 6:15** | **Demonstrating Weighted Load Balancing** | Terminal | Run reload command to switch to weighted config (`backend1 weight=3`, `backend2 weight=1`).<br>Run test with 40 requests.<br>*"Next, let's demonstrate weighted load balancing. We update Nginx configuration so `backend1` has a weight of 3 and `backend2` has a weight of 1, and reload Nginx on the fly with `nginx -s reload`. When we send 40 requests, `backend-1` receives approximately 75% of the traffic (30 requests) and `backend-2` receives 25% (10 requests), confirming weighted distribution."* |
| **6:15 - 7:00** | **Nginx Logs & Container Monitoring** | Terminal | Run `docker logs --tail 10 smart-home-nginx` and `docker stats --no-stream`.<br>*"Let's inspect the Nginx access logs. Each entry shows the exact upstream IP and port handling the request — alternating between `172.18.0.2:8080` and `172.18.0.3:8080`. Running `docker stats` displays real-time CPU and memory usage for each container, showing balanced resource consumption."* |
| **7:00 - 7:45** | **Conclusion** | Browser showing application UI / Terminal | *"To conclude, we have successfully containerized our Spring Boot application, orchestrated two identical backend instances, connected them to our host Oracle Database, and demonstrated both round-robin and weighted load balancing using Nginx with full logging and resource monitoring. Thank you."* |
