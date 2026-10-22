# start-local.ps1 - Launch Backend and Frontend in separate interactive PowerShell windows (Topology B: Host Native)
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Launching Smart Home IoT (Local Host Native Mode)       " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Start Spring Boot in a new window
Write-Host "[1/2] Launching Spring Boot Backend on :8080..." -ForegroundColor Yellow
$backendCmd = @"
cd '$projectRoot\backend'; `
`$env:DB_PASSWORD='SmartHomeDev123'; `
`$env:ADMIN_GOOGLE_EMAIL='akhilesh07vaidya@gmail.com'; `
`$env:GOOGLE_APPLICATION_CREDENTIALS='C:\Users\akhil\OneDrive\Desktop\firebase-secrets\smart-home-service-account.json'; `
.\mvnw.cmd spring-boot:run
"@
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $backendCmd

# 2. Start Vite Frontend in a new window
Write-Host "[2/2] Launching React / Vite Frontend on :5173..." -ForegroundColor Yellow
$frontendCmd = @"
cd '$projectRoot\frontend'; `
npm run dev
"@
Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $frontendCmd

Write-Host "Both processes launched in separate windows!" -ForegroundColor Green
Write-Host "Frontend: http://localhost:5173" -ForegroundColor Cyan
Write-Host "Backend:  http://localhost:8080" -ForegroundColor Cyan
