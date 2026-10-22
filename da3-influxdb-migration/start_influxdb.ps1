# start_influxdb.ps1
# Starts the isolated InfluxDB 2.x Docker container and waits until it is fully healthy.

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "`n[*] Starting isolated InfluxDB container (da3-influxdb-server)..." -ForegroundColor Cyan

# Ensure .env exists
if (-not (Test-Path ".env")) {
    Write-Host "[*] Creating .env from .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
}

# Start container
docker compose -f docker-compose.yml up -d

Write-Host "[*] Waiting for InfluxDB health probe on http://localhost:8086/health..." -NoNewline

$maxAttempts = 30
$attempt = 0
$isHealthy = $false

while ($attempt -lt $maxAttempts) {
    Start-Sleep -Seconds 1
    $attempt++
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8086/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $isHealthy = $true
            break
        }
    } catch {
        Write-Host "." -NoNewline
    }
}

Write-Host ""
if ($isHealthy) {
    Write-Host "[+] InfluxDB 2.7.11 is UP and HEALTHY on port 8086!" -ForegroundColor Green
    Write-Host "    Container: da3-influxdb-server" -ForegroundColor Gray
    Write-Host "    Web UI:    http://localhost:8086" -ForegroundColor Gray
    exit 0
} else {
    Write-Host "[!] InfluxDB failed to become healthy within 30 seconds." -ForegroundColor Red
    Write-Host "    Check logs via: docker logs da3-influxdb-server" -ForegroundColor Yellow
    exit 1
}
