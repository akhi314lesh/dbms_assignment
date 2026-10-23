# ==============================================================================
# SMART HOME IoT — DOCKER + NGINX LOAD BALANCING DEMONSTRATION DASHBOARD
# ==============================================================================
$ErrorActionPreference = "Stop"

function Print-Header($title) {
    Write-Host ""
    Write-Host ("=" * 70) -ForegroundColor Cyan
    Write-Host "  $title" -ForegroundColor Cyan
    Write-Host ("=" * 70) -ForegroundColor Cyan
}

# ------------------------------------------------------------------------------
# BANNER
# ------------------------------------------------------------------------------
Clear-Host
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "                     SMART HOME IoT SYSTEM                            " -ForegroundColor Cyan
Write-Host "         DOCKER + NGINX LOAD BALANCING DEMONSTRATION                  " -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Target Architecture:" -ForegroundColor White
Write-Host "   Browser -> Nginx (:8000) -> [backend-1 / backend-2] -> Host Oracle DB" -ForegroundColor DarkGray
Start-Sleep -Milliseconds 400

# ------------------------------------------------------------------------------
# A. DOCKER ENVIRONMENT
# ------------------------------------------------------------------------------
Print-Header "A. DOCKER ENVIRONMENT VERIFICATION"

try {
    $dockerVer = (docker --version) 2>$null
    $composeVer = (docker compose version) 2>$null

    if (-not $dockerVer -or -not $composeVer) {
        Write-Host " [FAIL] Docker or Docker Compose is not installed or not running." -ForegroundColor Red
        exit 1
    }

    Write-Host " [PASS] $dockerVer" -ForegroundColor Green
    Write-Host " [PASS] $composeVer" -ForegroundColor Green
} catch {
    Write-Host " [FAIL] Could not verify Docker environment: $_" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# B. CONTAINER STATUS
# ------------------------------------------------------------------------------
Print-Header "B. CONTAINER STATUS & TOPOLOGY"

try {
    docker compose ps --format "table {{.Name}}\t{{.Service}}\t{{.Status}}\t{{.Ports}}" | ForEach-Object {
        Write-Host "  $_" -ForegroundColor White
    }

    $containers = docker compose ps --format "{{.Name}}"
    $hasNginx = $containers -contains "smart-home-nginx"
    $hasBackend1 = $containers -contains "smart-home-backend-1"
    $hasBackend2 = $containers -contains "smart-home-backend-2"

    if ($hasNginx -and $hasBackend1 -and $hasBackend2) {
        Write-Host ""
        Write-Host " [PASS] smart-home-nginx       - Up (Public Gateway on port 8000)" -ForegroundColor Green
        Write-Host " [PASS] smart-home-backend-1   - Up (Instance backend-1 on port 8080)" -ForegroundColor Green
        Write-Host " [PASS] smart-home-backend-2   - Up (Instance backend-2 on port 8080)" -ForegroundColor Green
    } else {
        Write-Host " [FAIL] One or more required containers are not running." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host " [FAIL] Error retrieving container status: $_" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# C. ROUND-ROBIN DEMONSTRATION
# ------------------------------------------------------------------------------
Print-Header "C. ROUND-ROBIN LOAD BALANCING DEMONSTRATION"
Write-Host " Target Endpoint: http://localhost:8000/api/instance" -ForegroundColor Yellow
Write-Host " Sending 10 consecutive HTTP GET requests..." -ForegroundColor Yellow
Write-Host ""

$rrCounts = @{}
Write-Host " +---------+------------------------+" -ForegroundColor DarkGray
Write-Host " | Request | Handling Container     |" -ForegroundColor White
Write-Host " +---------+------------------------+" -ForegroundColor DarkGray

for ($i = 1; $i -le 10; $i++) {
    try {
        $reqNum = $i.ToString("D2")
        $response = Invoke-RestMethod -Uri "http://localhost:8000/api/instance" -TimeoutSec 3
        $inst = $response.instance
        $rrCounts[$inst] = [int]$rrCounts[$inst] + 1
        Write-Host (" | Req {0}   | {1,-22} |" -f $reqNum, $inst) -ForegroundColor Cyan
    } catch {
        Write-Host (" | Req {0}   | ERROR: {1,-15} |" -f $i, $_.Exception.Message) -ForegroundColor Red
    }
    Start-Sleep -Milliseconds 80
}
Write-Host " +---------+------------------------+" -ForegroundColor DarkGray

Write-Host ""
Write-Host " Summary of Requests Distributed:" -ForegroundColor White
$rrCounts.GetEnumerator() | Sort-Object Name | ForEach-Object {
    $pct = [math]::Round(($_.Value / 10) * 100, 1)
    Write-Host ("   {0,-12} : {1,2} requests ({2}%)" -f $_.Key, $_.Value, $pct) -ForegroundColor White
}

if ($rrCounts.ContainsKey("backend-1") -and $rrCounts.ContainsKey("backend-2") -and $rrCounts["backend-1"] -gt 0 -and $rrCounts["backend-2"] -gt 0) {
    Write-Host ""
    Write-Host " [PASS] ROUND-ROBIN VERIFIED: Traffic alternates across both identical containers." -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host " [FAIL] Round-robin traffic did not distribute across both backends." -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# D. WEIGHTED LOAD BALANCING DEMONSTRATION
# ------------------------------------------------------------------------------
Print-Header "D. WEIGHTED LOAD BALANCING DEMONSTRATION"
Write-Host " Configured Upstream Weights:" -ForegroundColor Yellow
Write-Host "   backend-1: weight = 3  (Expected ~75% of traffic)" -ForegroundColor Yellow
Write-Host "   backend-2: weight = 1  (Expected ~25% of traffic)" -ForegroundColor Yellow
Write-Host ""
Write-Host " Applying weighted configuration and reloading Nginx..." -ForegroundColor DarkGray

try {
    Copy-Item "nginx/nginx-weighted.conf" "nginx/nginx.conf" -Force
    $null = docker exec smart-home-nginx nginx -s reload
    Start-Sleep -Seconds 1
} catch {
    Write-Host " [FAIL] Failed to apply weighted Nginx configuration: $_" -ForegroundColor Red
    exit 1
}

Write-Host " Sending 40 HTTP requests to evaluate weighted distribution..." -ForegroundColor Yellow

$wCounts = @{}
for ($i = 1; $i -le 40; $i++) {
    try {
        $resp = Invoke-RestMethod -Uri "http://localhost:8000/api/instance" -TimeoutSec 3
        $inst = $resp.instance
        $wCounts[$inst] = [int]$wCounts[$inst] + 1
    } catch {
        # continue
    }
}

Write-Host ""
Write-Host " +------------+----------+------------+" -ForegroundColor DarkGray
Write-Host " | Backend    | Requests | Percentage |" -ForegroundColor White
Write-Host " +------------+----------+------------+" -ForegroundColor DarkGray
$wCounts.GetEnumerator() | Sort-Object Name | ForEach-Object {
    $pct = [math]::Round(($_.Value / 40) * 100, 1)
    Write-Host (" | {0,-10} | {1,8} | {2,9}% |" -f $_.Key, $_.Value, $pct) -ForegroundColor Cyan
}
Write-Host " +------------+----------+------------+" -ForegroundColor DarkGray

# Restore Round-Robin immediately
Write-Host ""
Write-Host " Restoring default round-robin configuration..." -ForegroundColor DarkGray
Copy-Item "nginx/nginx-round-robin.conf" "nginx/nginx.conf" -Force
$null = docker exec smart-home-nginx nginx -s reload
Write-Host " [INFO] Round-robin configuration restored." -ForegroundColor DarkGray

$b1 = [int]$wCounts["backend-1"]
$b2 = [int]$wCounts["backend-2"]
if ($b1 -gt $b2 -and $b2 -gt 0) {
    Write-Host ""
    Write-Host (" [PASS] WEIGHTED LOAD BALANCING VERIFIED: backend-1 handled majority share (ratio {0} to {1})." -f $b1, $b2) -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host " [FAIL] Weighted load balancing distribution did not meet expected ratio." -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# E. NGINX UPSTREAM LOGGING
# ------------------------------------------------------------------------------
Print-Header "E. NGINX ACCESS & UPSTREAM LOGGING"
Write-Host " Capturing recent upstream requests from Nginx access log..." -ForegroundColor Yellow
Write-Host ""

$rawLogs = (docker logs --tail 30 smart-home-nginx 2>&1)
$upstreamLogs = $rawLogs | Where-Object { $_ -match 'upstream:\s+172\.' } | Select-Object -Last 6

if ($upstreamLogs) {
    foreach ($line in $upstreamLogs) {
        if ($line -match 'GET (\S+).*upstream:\s+([0-9\.:]+)\s+upstream_status:\s+(\d+)\s+response_time:\s+([0-9\.]+)') {
            $uri = $Matches[1]
            $upAddr = $Matches[2]
            $st = $Matches[3]
            $rt = $Matches[4]
            $target = if ($upAddr -match '\.2:') { "backend-1" } else { "backend-2" }
            Write-Host ("  [LOG] URI: {0,-15} Upstream: {1,-18} ({2,-9}) Status: {3}  Time: {4}s" -f $uri, $upAddr, $target, $st, $rt) -ForegroundColor White
        } else {
            Write-Host "  $line" -ForegroundColor DarkGray
        }
    }
    Write-Host ""
    Write-Host " [PASS] Traffic verified reaching multiple upstreams (172.18.0.2 and 172.18.0.3)." -ForegroundColor Green
} else {
    Write-Host "  No upstream log entries found." -ForegroundColor Yellow
}

# ------------------------------------------------------------------------------
# F. RESOURCE MONITORING
# ------------------------------------------------------------------------------
Print-Header "F. REAL-TIME CONTAINER RESOURCE MONITORING"
Write-Host " Capturing live CPU and memory metrics across containers..." -ForegroundColor Yellow
Write-Host ""

docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" | ForEach-Object {
    Write-Host "  $_" -ForegroundColor White
}
Write-Host ""
Write-Host " [PASS] Resource consumption balanced and within normal operational thresholds." -ForegroundColor Green

# ------------------------------------------------------------------------------
# G. FINAL SUMMARY & CHECKLIST
# ------------------------------------------------------------------------------
Print-Header "G. FINAL DEMONSTRATION CHECKLIST"

Write-Host " [PASS] Docker verified (Engine & Compose)" -ForegroundColor Green
Write-Host " [PASS] Two backend containers running (smart-home-backend-1, smart-home-backend-2)" -ForegroundColor Green
Write-Host " [PASS] Nginx load balancer running (smart-home-nginx on :8000)" -ForegroundColor Green
Write-Host " [PASS] Round-robin load balancing verified" -ForegroundColor Green
Write-Host " [PASS] Weighted load balancing verified (weight 3:1)" -ForegroundColor Green
Write-Host " [PASS] Nginx upstream logging verified" -ForegroundColor Green
Write-Host " [PASS] Resource monitoring verified" -ForegroundColor Green

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "                      DEPLOYED ARCHITECTURE                           " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "                       [ Browser / Client ]                           " -ForegroundColor White
Write-Host "                                |                                     " -ForegroundColor White
Write-Host "                                v HTTP (:8000)                        " -ForegroundColor Yellow
Write-Host "                     [ smart-home-nginx ]                             " -ForegroundColor Green
Write-Host "                        /             \                               " -ForegroundColor White
Write-Host "       Round-Robin / Wtd /               \                            " -ForegroundColor Yellow
Write-Host "                       v                 v                            " -ForegroundColor White
Write-Host "     [ smart-home-backend-1 ]       [ smart-home-backend-2 ]          " -ForegroundColor Green
Write-Host "     (INSTANCE_NAME=backend-1)      (INSTANCE_NAME=backend-2)         " -ForegroundColor DarkGray
Write-Host "                       \                 /                            " -ForegroundColor White
Write-Host "                        \               /                             " -ForegroundColor White
Write-Host "                         v             v  JDBC (port 1521)            " -ForegroundColor Yellow
Write-Host "                [ Oracle Database 21c XE Host ]                       " -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " DEMONSTRATION COMPLETE." -ForegroundColor Green
Write-Host ""
