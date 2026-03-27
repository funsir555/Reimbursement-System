@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

cd /d "%~dp0"

where git >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Git is not installed or not in PATH.
  pause
  exit /b 1
)

for /f "delims=" %%i in ('git rev-parse --is-inside-work-tree 2^>nul') do set "IN_REPO=%%i"
if /i not "!IN_REPO!"=="true" (
  echo [ERROR] Current folder is not a Git repository.
  pause
  exit /b 1
)

for /f "delims=" %%i in ('git branch --show-current 2^>nul') do set "BRANCH=%%i"
if not defined BRANCH (
  echo [ERROR] Could not detect current branch.
  pause
  exit /b 1
)

for /f "delims=" %%i in ('git remote get-url origin 2^>nul') do set "REMOTE_URL=%%i"
if not defined REMOTE_URL (
  echo [ERROR] Remote "origin" is not configured.
  pause
  exit /b 1
)

echo.
echo Repository: %cd%
echo Branch: !BRANCH!
echo Remote: !REMOTE_URL!
echo.

set "DEFAULT_MSG=auto sync %date% %time%"
set "COMMIT_MSG=!DEFAULT_MSG!"
echo Commit message: !COMMIT_MSG!

echo.
echo [1/3] Staging changes...
git add -A
if errorlevel 1 (
  echo [ERROR] git add failed.
  pause
  exit /b 1
)

git diff --cached --quiet
if not errorlevel 1 (
  echo [INFO] No changes to commit.
  pause
  exit /b 0
)

echo [2/3] Creating commit...
git commit -m "!COMMIT_MSG!"
if errorlevel 1 (
  echo [ERROR] git commit failed.
  echo Check the output above, then try again.
  pause
  exit /b 1
)

echo [3/3] Pushing to origin/!BRANCH!...
git push -u origin !BRANCH!
if errorlevel 1 (
  echo [ERROR] git push failed.
  echo If the remote has new commits, pull/rebase first and resolve conflicts before retrying.
  pause
  exit /b 1
)

echo.
echo [DONE] Push completed successfully.
pause
