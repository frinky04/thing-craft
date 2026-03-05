param(
    [string]$Path = "",
    [int]$TopWorst = 8
)

$ErrorActionPreference = "Stop"

function Get-PercentileValue {
    param(
        [double[]]$Values,
        [double]$Percentile
    )
    if ($Values.Count -eq 0) {
        return [double]::NaN
    }
    $sorted = $Values | Sort-Object
    $index = [Math]::Ceiling(($Percentile / 100.0) * $sorted.Count) - 1
    if ($index -lt 0) { $index = 0 }
    if ($index -ge $sorted.Count) { $index = $sorted.Count - 1 }
    return [double]$sorted[$index]
}

if ([string]::IsNullOrWhiteSpace($Path)) {
    $latest = Get-ChildItem "docs/reports/benchmarks" -Filter "bench_*.csv" -File |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
    if ($null -eq $latest) {
        throw "No benchmark CSV files found in docs/reports/benchmarks"
    }
    $Path = $latest.FullName
}

if (!(Test-Path $Path)) {
    throw "Benchmark CSV not found: $Path"
}

$rows = Import-Csv $Path
if ($rows.Count -eq 0) {
    throw "Benchmark CSV is empty: $Path"
}

Write-Host "Benchmark summary: $Path"
Write-Host "Total samples: $($rows.Count)"
Write-Host ""

$phaseStats = $rows |
    Group-Object phase |
    ForEach-Object {
        $phaseRows = $_.Group
        $frameValues = @($phaseRows | ForEach-Object { [double]$_.avg_frame_ms })
        $fpsValues = @($phaseRows | ForEach-Object { [double]$_.fps })
        [pscustomobject]@{
            phase = $_.Name
            samples = $phaseRows.Count
            avg_fps = [Math]::Round((($fpsValues | Measure-Object -Average).Average), 2)
            min_fps = [Math]::Round((($fpsValues | Measure-Object -Minimum).Minimum), 2)
            avg_frame_ms = [Math]::Round((($frameValues | Measure-Object -Average).Average), 3)
            p95_frame_ms = [Math]::Round((Get-PercentileValue -Values $frameValues -Percentile 95), 3)
            max_frame_ms = [Math]::Round((($frameValues | Measure-Object -Maximum).Maximum), 3)
            avg_tick_ms = [Math]::Round((($phaseRows | Measure-Object avg_tick_ms -Average).Average), 3)
            avg_ready_chunks = [Math]::Round((($phaseRows | Measure-Object ready_chunks -Average).Average), 1)
            avg_meshing_chunks = [Math]::Round((($phaseRows | Measure-Object meshing_chunks -Average).Average), 1)
            avg_visible_chunks = [Math]::Round((($phaseRows | Measure-Object visible_chunks -Average).Average), 1)
        }
    } |
    Sort-Object phase

$phaseStats | Format-Table -AutoSize

Write-Host ""
Write-Host "Worst frame samples (by avg_frame_ms):"
$rows |
    Sort-Object { [double]$_.avg_frame_ms } -Descending |
    Select-Object -First $TopWorst phase, elapsed_s, fps, avg_frame_ms, avg_tick_ms, ready_chunks, meshing_chunks, visible_chunks |
    Format-Table -AutoSize
