# stop_influxdb.ps1
# Stops the isolated InfluxDB 2.x Docker container without affecting any other services.

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "`n[*] Stopping isolated InfluxDB container (da3-influxdb-server)..." -ForegroundColor Cyan
docker compose -f docker-compose.yml down

Write-Host "[+] InfluxDB container stopped." -ForegroundColor Green
Write-Host "    Note: Telemetry data is preserved in volume 'da3-influxdb-storage'." -ForegroundColor Gray
Write-Host "    To completely wipe container volume, run: docker compose -f docker-compose.yml down -v`n" -ForegroundColor Gray
