# check_prerequisites.ps1
# Checks system readiness for running the isolated InfluxDB component on Windows.

$ErrorActionPreference = "Continue"

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   DA3 InfluxDB Component - Prerequisites Check        " -ForegroundColor Cyan
Write-Host "=======================================================`n" -ForegroundColor Cyan

$allPassed = $true

# 1. Check Python
Write-Host "[1/4] Checking Python 3..." -NoNewline
try {
    $pythonVersion = python --version 2>&1
    if ($LASTEXITCODE -eq 0 -and $pythonVersion -match "Python 3") {
        Write-Host " [PASS] ($pythonVersion)" -ForegroundColor Green
    } else {
        Write-Host " [FAIL]" -ForegroundColor Red
        Write-Host "       Python 3 is required. Install via: winget install Python.Python.3.11" -ForegroundColor Yellow
        Write-Host "       Or download from: https://www.python.org/downloads/" -ForegroundColor Yellow
        $allPassed = $false
    }
} catch {
    Write-Host " [FAIL] (Python not found in PATH)" -ForegroundColor Red
    Write-Host "       Install via: winget install Python.Python.3.11" -ForegroundColor Yellow
    $allPassed = $false
}

# 2. Check Docker CLI
Write-Host "[2/4] Checking Docker CLI..." -NoNewline
try {
    $dockerVer = docker --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host " [PASS] ($dockerVer)" -ForegroundColor Green
    } else {
        Write-Host " [FAIL]" -ForegroundColor Red
        Write-Host "       Docker is required. Install Docker Desktop: https://www.docker.com/products/docker-desktop/" -ForegroundColor Yellow
        Write-Host "       Or via winget: winget install Docker.DockerDesktop" -ForegroundColor Yellow
        $allPassed = $false
    }
} catch {
    Write-Host " [FAIL] (Docker not found in PATH)" -ForegroundColor Red
    Write-Host "       Install Docker Desktop: https://www.docker.com/products/docker-desktop/" -ForegroundColor Yellow
    $allPassed = $false
}

# 3. Check Docker Engine / Daemon Status
Write-Host "[3/4] Checking Docker Daemon status..." -NoNewline
try {
    $null = docker info 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host " [PASS] (Docker Daemon is running)" -ForegroundColor Green
    } else {
        Write-Host " [FAIL] (Docker Desktop is installed but not running)" -ForegroundColor Red
        Write-Host "       Please launch Docker Desktop from the Windows Start menu and wait until it is ready." -ForegroundColor Yellow
        $allPassed = $false
    }
} catch {
    Write-Host " [FAIL]" -ForegroundColor Red
    $allPassed = $false
}

# 4. Check Port 8086 Availability
Write-Host "[4/4] Checking Port 8086 availability..." -NoNewline
$portInUse = Get-NetTCPConnection -LocalPort 8086 -ErrorAction SilentlyContinue
if ($null -eq $portInUse) {
    Write-Host " [PASS] (Port 8086 is free)" -ForegroundColor Green
} else {
    # Check if our own container is already using it
    $containerName = docker ps --filter "name=da3-influxdb-server" --format "{{.Names}}" 2>$null
    if ($containerName -eq "da3-influxdb-server") {
        Write-Host " [PASS] (Port 8086 is currently held by da3-influxdb-server)" -ForegroundColor Green
    } else {
        Write-Host " [WARN] (Port 8086 is occupied by PID $($portInUse[0].OwningProcess))" -ForegroundColor Yellow
        Write-Host "        You can change the host port by setting INFLUXDB_PORT=8087 in .env" -ForegroundColor Yellow
    }
}

# 5. Environment configuration check
if (-not (Test-Path ".env")) {
    Write-Host "`n[*] Notice: Local '.env' not found. It will be automatically generated from '.env.example' upon first run." -ForegroundColor Cyan
}

Write-Host "`n-------------------------------------------------------"
if ($allPassed) {
    Write-Host "[RESULT] All prerequisites satisfied! Ready to run demo." -ForegroundColor Green
    exit 0
} else {
    Write-Host "[RESULT] Some prerequisites are missing. Please review the items marked [FAIL] above." -ForegroundColor Red
    exit 1
}
