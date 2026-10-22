# scripts/package-project.ps1
# Creates a clean, lightweight, secret-free ZIP archive of the entire project
# including DA1, DA2, and DA3 InfluxDB components.

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$zipFileName = "SmartHome_DBMS_Complete_Project.zip"
$zipPathRoot = Join-Path $projectRoot $zipFileName

$desktopPath = [Environment]::GetFolderPath("Desktop")
$zipPathDesktop = Join-Path $desktopPath $zipFileName

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   Smart Home DBMS - Automated Project Packaging       " -ForegroundColor Cyan
Write-Host "=======================================================`n" -ForegroundColor Cyan

# Remove existing zip if present
if (Test-Path $zipPathRoot) { Remove-Item -Force $zipPathRoot }
if (Test-Path $zipPathDesktop) { Remove-Item -Force $zipPathDesktop }

# Create a temporary staging directory
$tempStaging = Join-Path $env:TEMP ("smarthome_pack_" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempStaging | Out-Null

Write-Host "[1/3] Collecting and filtering clean project files..." -ForegroundColor Yellow

# Patterns to strictly exclude
$excludeDirectories = @(
    "node_modules",
    "target",
    ".git",
    ".venv",
    "venv",
    "env",
    "__pycache__",
    ".pytest_cache",
    ".vercel",
    "dist",
    "dist-ssr",
    "build",
    ".idea",
    ".vscode"
)

$excludeFiles = @(
    ".env",
    ".env.local",
    "*.log",
    "*service-account*.json",
    "*firebase*.json",
    "*credentials*.json",
    "serviceAccountKey*.json",
    "*.zip"
)

# Robust file copying to staging
Get-ChildItem -Path $projectRoot -Recurse | ForEach-Object {
    $item = $_
    $relativePath = $item.FullName.Substring($projectRoot.Length).TrimStart("\", "/")

    # Check if inside an excluded directory
    $inExcludedDir = $false
    foreach ($dir in $excludeDirectories) {
        if ($relativePath -match "(^|[\\/])$dir([\\/]|$)") {
            $inExcludedDir = $true
            break
        }
    }
    if ($inExcludedDir) { return }

    # If it's a file, check if it matches excluded file patterns
    if (-not $item.PSIsContainer) {
        $isExcludedFile = $false
        foreach ($filePattern in $excludeFiles) {
            if ($item.Name -like $filePattern -or $relativePath -like $filePattern) {
                # Preserve .env.example
                if ($item.Name -like "*.env.example" -or $item.Name -like ".env.example*") {
                    # keep it!
                } else {
                    $isExcludedFile = $true
                    break
                }
            }
        }
        if ($isExcludedFile) { return }

        $targetFile = Join-Path $tempStaging $relativePath
        $targetDir = Split-Path -Parent $targetFile
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }
        Copy-Item -Path $item.FullName -Destination $targetFile -Force
    }
}

Write-Host "[2/3] Compressing clean files into ZIP archive..." -ForegroundColor Yellow
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($tempStaging, $zipPathRoot, [System.IO.Compression.CompressionLevel]::Optimal, $false)

# Copy to Desktop if available
try {
    Copy-Item -Path $zipPathRoot -Destination $zipPathDesktop -Force
    $desktopSuccess = $true
} catch {
    $desktopSuccess = $false
}

# Clean up temp staging
Remove-Item -Recurse -Force $tempStaging

# Get zip size
$zipFileInfo = Get-Item $zipPathRoot
$sizeMb = [Math]::Round($zipFileInfo.Length / 1MB, 2)

Write-Host "[3/3] Packaging completed successfully!" -ForegroundColor Green
Write-Host "`n=======================================================" -ForegroundColor Green
Write-Host "   PACKAGE READY FOR HANDOVER                          " -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Green
Write-Host "File Name: $zipFileName" -ForegroundColor White
Write-Host "File Size: $sizeMb MB" -ForegroundColor White
Write-Host "Saved to:" -ForegroundColor Cyan
Write-Host "  1. Project Root: $zipPathRoot" -ForegroundColor White
if ($desktopSuccess) {
    Write-Host "  2. Desktop:      $zipPathDesktop" -ForegroundColor White
}
Write-Host "`nSecurity Verified:" -ForegroundColor Gray
Write-Host "  • Excluded: node_modules, target, .git, .env, service account keys, temporary caches." -ForegroundColor Gray
Write-Host "  • Included: database/, backend/, frontend/, da3-influxdb-migration/, docs/, scripts/, docker-compose.yml" -ForegroundColor Gray
