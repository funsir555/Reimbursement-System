@echo off
setlocal EnableExtensions

for %%I in ("%~dp0.") do set "SOURCE_ROOT=%%~fI"
for %%I in ("%SOURCE_ROOT%") do set "SOURCE_NAME=%%~nxI"

pushd "%SOURCE_ROOT%\.." >nul
set "PARENT_DIR=%CD%"
popd >nul

set "SHADOW_ROOT=%PARENT_DIR%\%SOURCE_NAME%-shadow"
set "SHADOW_ENV_PATH=%SHADOW_ROOT%\backend\.env.shadow.cmd"

if /I "%SOURCE_ROOT%"=="%SHADOW_ROOT%" (
  echo [shadow] Shadow workspace path must differ from the source workspace path.
  exit /b 1
)

if not exist "%SHADOW_ROOT%" (
  mkdir "%SHADOW_ROOT%"
)

echo [shadow] Syncing workspace snapshot to %SHADOW_ROOT%
robocopy "%SOURCE_ROOT%" "%SHADOW_ROOT%" /MIR /R:1 /W:1 /NFL /NDL /NJH /NJS /NP ^
  /XD .git node_modules target .idea .vscode storage ^
  /XF *.log *.tmp tmp-admin-token.txt

if errorlevel 8 (
  echo [shadow] Robocopy failed with exit code %errorlevel%.
  exit /b %errorlevel%
)

if not exist "%SHADOW_ROOT%\backend" (
  mkdir "%SHADOW_ROOT%\backend"
)

(
  echo @echo off
  echo for %%%%I in ^("%%~dp0.."^) do set "SHADOW_ROOT=%%%%~fI"
  echo set "SOURCE_ROOT=%%SHADOW_ROOT:~0,-7%%"
  echo set FINEX_GATEWAY_PORT=8082
  echo set FINEX_AUTH_SERVICE_PORT=8083
  echo set FINEX_AUTH_SERVICE_URL=http://localhost:8083
  echo set FINEX_FRONTEND_PORT=5174
  echo set FINEX_GATEWAY_PROXY_TARGET=http://localhost:8082
  echo set "FINEX_EXPENSE_ATTACHMENT_STORAGE_PATH=%%SOURCE_ROOT%%\backend\auth-service\storage\expense-attachments"
) > "%SHADOW_ENV_PATH%"

echo [shadow] Refreshed %SHADOW_ENV_PATH%
echo [shadow] Shadow workspace is ready. Run start-finex.bat inside the shadow directory when needed.
