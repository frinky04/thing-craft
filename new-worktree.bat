@echo off
set /p NAME="Enter worktree/branch name: "
if "%NAME%"=="" (
    echo No name entered. Aborting.
    exit /b 1
)
git worktree add -f "../project-%NAME%" -b "%NAME%"
echo.
echo To enter the worktree run:
echo   cd "../project-%NAME%"