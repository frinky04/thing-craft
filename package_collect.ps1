param(
    [string]$ProjectRoot,
    [string]$Profile = "release"
)

$srcDir  = Join-Path $ProjectRoot "thingcraft-client\src"
$distDir = Join-Path $ProjectRoot "dist\thingcraft"

# Scan all .rs files for resource path references
Write-Host "==> Scanning source for referenced resources..."
$paths = @{}
Get-ChildItem -Path $srcDir -Filter "*.rs" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    # Match resource paths that end with a file extension (e.g. .png)
    [regex]::Matches($content, 'resources/[\w\-./]+\.\w+') | ForEach-Object {
        $paths[$_.Value] = $true
    }
}

$resourceList = $paths.Keys | Sort-Object
Write-Host "    Found $($resourceList.Count) resource files referenced in source."

# Clean and recreate dist dir
if (Test-Path $distDir) { Remove-Item $distDir -Recurse -Force }
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

# Copy binary
$exe = Join-Path $ProjectRoot "target\$Profile\thingcraft-client.exe"
if (-not (Test-Path $exe)) {
    Write-Host "ERROR: Binary not found at $exe"
    exit 1
}
Copy-Item $exe -Destination $distDir
Write-Host "==> Copied thingcraft-client.exe"

# Copy only referenced resources
$copied = 0
$missing = 0
foreach ($relPath in $resourceList) {
    $srcFile  = Join-Path $ProjectRoot $relPath
    $destFile = Join-Path $distDir $relPath

    if (Test-Path $srcFile) {
        $destDir = Split-Path $destFile -Parent
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        Copy-Item $srcFile -Destination $destFile
        $copied++
        Write-Host "    $relPath"
    } else {
        Write-Host "    WARNING: not found: $relPath"
        $missing++
    }
}

Write-Host "==> Copied $copied resource files ($missing missing)"

# Create zip
$zipPath = Join-Path $ProjectRoot "dist\thingcraft.zip"
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Write-Host "==> Creating zip..."
Compress-Archive -Path $distDir -DestinationPath $zipPath
Write-Host "==> Package created: dist\thingcraft.zip"
Write-Host "==> Done!"
