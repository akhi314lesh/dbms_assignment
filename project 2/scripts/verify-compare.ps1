# verify-compare.ps1 - End-to-end check of the Oracle vs InfluxDB comparison backend.
# Run AFTER the backend is up:   powershell -ExecutionPolicy Bypass -File .\scripts\verify-compare.ps1
$ErrorActionPreference = "Stop"
$base = if ($env:API_URL) { $env:API_URL } else { "http://localhost:8080" }

function Post($path, $body) {
    Invoke-RestMethod -Method Post -Uri "$base$path" -ContentType "application/json" -Body ($body | ConvertTo-Json) -TimeoutSec 600
}

Write-Host "1) Health" -ForegroundColor Cyan
$h = Invoke-RestMethod "$base/api/compare/health"
"   Oracle  : {0}  {1}{2}" -f $h.oracle, $h.oracleVersion, $h.oracleError
"   InfluxDB: {0}  {1}{2}" -f $h.influx, $h.influxVersion, $h.influxError
if (-not ($h.oracle -and $h.influx)) { Write-Host "   Both databases must be UP. Fix the error above first." -ForegroundColor Red; exit 1 }

Write-Host "2) Generate a small identical dataset in both databases (1 day @ 5 min)" -ForegroundColor Cyan
$s = Post "/api/compare/seed" @{ days = 1; intervalMinutes = 5 }
"   {0} rows  | Oracle write {1} ms | InfluxDB write {2} ms | in sync: {3}" -f $s.rowsGenerated, $s.oracleWriteMs, $s.influxWriteMs, $s.dataset.inSync

Write-Host "3) Run every workload (3 timed runs each)" -ForegroundColor Cyan
$all = Post "/api/compare/run-all" @{ iterations = 3; writeRows = 1000 }
$failed = 0
foreach ($r in $all.results) {
    if ($r.error) { $failed++; "   [ERROR] {0}: {1}" -f $r.label, $r.error }
    else {
        $ok = if ($r.matchStatus -eq "MATCH") { "OK " } else { $failed++; "!! " }
        "   [{0}] {1,-34} Oracle {2,9} ms | InfluxDB {3,9} ms | faster: {4,-6} | results: {5}" -f $ok, $r.label, $r.oracleMs, $r.influxMs, $r.winner, $r.matchStatus
    }
}
Write-Host ""
Write-Host $all.analysis
if ($failed -eq 0) { Write-Host "ALL CHECKS PASSED" -ForegroundColor Green } else { Write-Host "$failed workload(s) need attention (see above)" -ForegroundColor Red; exit 1 }
