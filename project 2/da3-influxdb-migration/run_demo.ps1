# run_demo.ps1
# All-In-One Automated Demonstration Script for DA3 InfluxDB Migration.
# Can be run on any Windows system with Python 3 and Docker installed.

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   DA3: Isolated InfluxDB Migration & Verification     " -ForegroundColor Cyan
Write-Host "=======================================================`n" -ForegroundColor Cyan

# Step 1: Check Prerequisites
Write-Host ">>> STEP 1: Verifying Environment Prerequisites..." -ForegroundColor Yellow
$prereqResult = & ".\check_prerequisites.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[!] Prerequisites check failed. Please resolve the issues above before proceeding." -ForegroundColor Red
    exit 1
}

# Step 2: Initialize .env if missing
if (-not (Test-Path ".env")) {
    Write-Host "`n>>> STEP 2: Creating local .env from .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "[+] Created .env with default local settings." -ForegroundColor Green
}

# Step 3: Start InfluxDB Container
Write-Host "`n>>> STEP 3: Starting Isolated InfluxDB 2.x Container..." -ForegroundColor Yellow
& ".\start_influxdb.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Failed to start InfluxDB container." -ForegroundColor Red
    exit 1
}

# Step 4: Execute Data Migration
Write-Host "`n>>> STEP 4: Ingesting Sample Sensor Dataset into InfluxDB..." -ForegroundColor Yellow
python scripts/migrate_readings.py
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Data migration failed." -ForegroundColor Red
    exit 1
}

# Step 5: Execute the Three Analytical Queries
Write-Host "`n>>> STEP 5: Executing the 3 Analytical Time-Series Flux Queries..." -ForegroundColor Yellow
python scripts/query_readings.py --query all --device 4
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Query execution failed." -ForegroundColor Red
    exit 1
}

# Step 6: Run Automated Validation Test Suite
Write-Host "`n>>> STEP 6: Running Complete Validation Test Suite..." -ForegroundColor Yellow
python -m unittest tests/test_sensor_migration.py -v
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Validation tests failed." -ForegroundColor Red
    exit 1
}

# Step 7: Completed
Write-Host "`n=======================================================" -ForegroundColor Green
Write-Host "   [SUCCESS] DEMONSTRATION & VERIFICATION COMPLETE!    " -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Green
Write-Host "• 60 records successfully migrated and verified in InfluxDB." -ForegroundColor White
Write-Host "• All 3 analytical Flux queries executed with expected outputs." -ForegroundColor White
Write-Host "• Unit & live integration test suite passed cleanly." -ForegroundColor White
Write-Host "`nTo stop the isolated InfluxDB container when finished, run:" -ForegroundColor Cyan
Write-Host "    .\stop_influxdb.ps1`n" -ForegroundColor White
