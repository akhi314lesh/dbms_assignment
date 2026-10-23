# Switch to Weighted Config, Test 40 requests, and Restore
Write-Host "Switching to weighted configuration (backend1 weight=3, backend2 weight=1)..." -ForegroundColor Cyan
Copy-Item nginx/nginx-weighted.conf nginx/nginx.conf -Force
docker exec smart-home-nginx nginx -s reload
Start-Sleep -Seconds 1

Write-Host "Running 40 requests to measure traffic distribution..." -ForegroundColor Cyan
$counts = @{}
1..40 | ForEach-Object {
    $res = (Invoke-RestMethod -Uri "http://localhost:8000/api/instance").instance
    $counts[$res] = [int]$counts[$res] + 1
}

Write-Host "TOTAL REQUESTS: 40"
$counts.GetEnumerator() | Sort-Object Name | ForEach-Object {
    $pct = [math]::Round(($_.Value / 40) * 100, 1)
    Write-Host "$($_.Key): $($_.Value) requests ($pct%)"
}

Write-Host "Restoring round-robin configuration..." -ForegroundColor Cyan
Copy-Item nginx/nginx-round-robin.conf nginx/nginx.conf -Force
docker exec smart-home-nginx nginx -s reload
Write-Host "Restored." -ForegroundColor Green
