# Test Round Robin Load Balancing (10 requests)
Write-Host "Testing Round-Robin Load Balancing on http://localhost:8000/api/instance..." -ForegroundColor Cyan
1..10 | ForEach-Object {
    $res = Invoke-RestMethod -Uri "http://localhost:8000/api/instance"
    Write-Host "Request $_ -> $($res.instance)"
}
