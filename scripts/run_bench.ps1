param(
    [int]$ViewRadius = 16,
    [int]$StillSecs = 10,
    [int]$TurnSecs = 10,
    [int]$MoveSecs = 10,
    [double]$TurnPxPerSec = 240.0,
    [switch]$NoSummary
)

$ErrorActionPreference = "Stop"

$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$outDir = "docs/reports/benchmarks"
$outFile = Join-Path $outDir ("bench_{0}.csv" -f $stamp)

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$env:THINGCRAFT_BENCH = "1"
$env:THINGCRAFT_BENCH_STILL_SECS = "$StillSecs"
$env:THINGCRAFT_BENCH_TURN_SECS = "$TurnSecs"
$env:THINGCRAFT_BENCH_MOVE_SECS = "$MoveSecs"
$env:THINGCRAFT_BENCH_TURN_PX_PER_SEC = "$TurnPxPerSec"
$env:THINGCRAFT_BENCH_OUTPUT = $outFile
$env:THINGCRAFT_VIEW_RADIUS = "$ViewRadius"

Write-Host "Running benchmark..."
Write-Host "  output: $outFile"
Write-Host "  view radius: $ViewRadius"
Write-Host "  phases: still=$StillSecs turn=$TurnSecs move=$MoveSecs"

cargo run --release -p thingcraft-client

Write-Host "Benchmark finished: $outFile"
if (-not $NoSummary) {
    Write-Host ""
    & "$PSScriptRoot/summarize_bench.ps1" -Path $outFile
}
