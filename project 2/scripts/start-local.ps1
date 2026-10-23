# start-local.ps1 - Launch InfluxDB, the Spring Boot backend and the 3 DA3 websites.
# Usage (from the project root):   powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Launching Smart Home IoT DA3 (Oracle vs InfluxDB)       " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# ── Prerequisite checks ────────────────────────────────────────────────
foreach ($tool in @("java", "node", "npm")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        Write-Host "[X] '$tool' was not found in PATH. Install it first (see RUN_GUIDE.md)." -ForegroundColor Red
        exit 1
    }
}

# ── Oracle password ────────────────────────────────────────────────────
$dbPassword = $env:DB_PASSWORD
if (-not $dbPassword) {
    $dbPassword = Read-Host "Enter the Oracle password of user SMART_HOME (press Enter for 'SmartHomeDev123')"
    if (-not $dbPassword) { $dbPassword = "SmartHomeDev123" }
}

# ── 1. InfluxDB (Docker) ───────────────────────────────────────────────
Write-Host "[1/5] Starting InfluxDB 2.7 on :8086 (Docker)..." -ForegroundColor Yellow
$influxUp = $false
try { $influxUp = (Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 http://localhost:8086/health).StatusCode -eq 200 } catch {}
if (-not $influxUp) {
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        Push-Location "$projectRoot\da3-influxdb-migration"
        docker compose up -d
        Pop-Location
        for ($i = 0; $i -lt 30 -and -not $influxUp; $i++) {
            Start-Sleep -Seconds 2
            try { $influxUp = (Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 http://localhost:8086/health).StatusCode -eq 200 } catch {}
        }
    }
}
if ($influxUp) { Write-Host "      InfluxDB is healthy." -ForegroundColor Green }
else { Write-Host "      InfluxDB is NOT reachable. Start Docker Desktop, then run: cd da3-influxdb-migration; docker compose up -d" -ForegroundColor Red }

# ── 2. npm install where needed ────────────────────────────────────────
foreach ($site in @("frontend", "frontend-influx", "frontend-compare")) {
    if (-not (Test-Path "$projectRoot\$site\node_modules")) {
        Write-Host "[2/5] npm install in $site (first run only)..." -ForegroundColor Yellow
        Push-Location "$projectRoot\$site"
        npm install --no-audit --no-fund
        Pop-Location
    }
}

# ── 3. Backend ─────────────────────────────────────────────────────────
Write-Host "[3/5] Launching Spring Boot backend on :8080 (new window)..." -ForegroundColor Yellow
$backendCmd = @"
cd '$projectRoot\backend'
`$env:DB_PASSWORD='$dbPassword'
.\mvnw.cmd spring-boot:run
"@
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $backendCmd

# ── 4. Websites ────────────────────────────────────────────────────────
Write-Host "[4/5] Launching the three websites (new windows)..." -ForegroundColor Yellow
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\frontend'; npm run dev -- --port 5173 --strictPort"
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\frontend-influx'; npm run dev"
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\frontend-compare'; npm run dev"

# ── 5. Wait for the backend, then open the comparison dashboard ────────
Write-Host "[5/5] Waiting for the backend to come up (first start downloads Maven dependencies)..." -ForegroundColor Yellow
$backendUp = $false
for ($i = 0; $i -lt 90 -and -not $backendUp; $i++) {
    Start-Sleep -Seconds 3
    try { $backendUp = (Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 http://localhost:8080/api/compare/health).StatusCode -eq 200 } catch {}
}
if ($backendUp) {
    $h = Invoke-RestMethod http://localhost:8080/api/compare/health
    Write-Host ("      Backend up. Oracle: {0}  InfluxDB: {1}" -f $(if ($h.oracle) {"UP"} else {"DOWN - " + $h.oracleError}), $(if ($h.influx) {"UP"} else {"DOWN - " + $h.influxError})) -ForegroundColor Green
    Start-Process "http://localhost:5175"
} else {
    Write-Host "      Backend not up yet - check the backend window for errors." -ForegroundColor Red
}

Write-Host ""
Write-Host "Oracle Smart Home:     http://localhost:5173" -ForegroundColor Cyan
Write-Host "InfluxDB Smart Home:   http://localhost:5174" -ForegroundColor Cyan
Write-Host "Comparison Dashboard:  http://localhost:5175" -ForegroundColor Cyan
Write-Host "Spring Boot Backend:   http://localhost:8080" -ForegroundColor Cyan
