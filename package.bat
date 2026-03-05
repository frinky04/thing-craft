@echo off
:: Build a distributable package with only the resources actually used by the code.
:: Usage: package.bat [--debug]
:: Output: dist\thingcraft.zip

setlocal

set "PROFILE=release"
set "BUILD_FLAG=--release"
if "%~1"=="--debug" (
    set "PROFILE=debug"
    set "BUILD_FLAG="
)

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

echo ==^> Building thingcraft-client (%PROFILE%)...
cd /d "%PROJECT_ROOT%"
cargo build %BUILD_FLAG% -p thingcraft-client
if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

echo ==^> Scanning and packaging...
powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\package_collect.ps1" "%PROJECT_ROOT%" "%PROFILE%"
exit /b %errorlevel%
