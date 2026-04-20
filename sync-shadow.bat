@echo off
setlocal EnableExtensions

for %%I in ("%~dp0.") do set "SOURCE_ROOT=%%~fI"
for %%I in ("%SOURCE_ROOT%") do set "SOURCE_NAME=%%~nxI"

if /I "%SOURCE_NAME:~-7%"=="-shadow" (
  echo [shadow] Please run sync-shadow.bat from the main workspace, not from the shadow workspace.
  exit /b 1
)

pushd "%SOURCE_ROOT%\.." >nul
set "PARENT_DIR=%CD%"
popd >nul

set "SHADOW_ROOT=%PARENT_DIR%\%SOURCE_NAME%-shadow"
set "SHADOW_BACKEND_DIR=%SHADOW_ROOT%\backend"
set "SHADOW_ENV_PATH=%SHADOW_BACKEND_DIR%\.env.shadow.cmd"
set "SHADOW_START_PATH=%SHADOW_ROOT%\start-finex.bat"
set "SHADOW_SYNC_HINT_PATH=%SHADOW_ROOT%\sync-shadow.bat"
set "SOURCE_FRONTEND_DIR=%SOURCE_ROOT%\frontend\admin-web"
set "SHADOW_FRONTEND_DIR=%SHADOW_ROOT%\frontend\admin-web"
set "SHADOW_FRONTEND_STAMP_PATH=%SHADOW_FRONTEND_DIR%\.shadow-deps.hash"

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
  /XF *.log *.tmp tmp-admin-token.txt start-finex.bat sync-shadow.bat .env.shadow.cmd .shadow-deps.hash

if errorlevel 8 (
  echo [shadow] Robocopy failed with exit code %errorlevel%.
  exit /b %errorlevel%
)

if not exist "%SHADOW_BACKEND_DIR%" (
  mkdir "%SHADOW_BACKEND_DIR%"
)

(
  echo @echo off
  echo set FINEX_FRONTEND_PORT=5174
  echo set FINEX_GATEWAY_PORT=8082
  echo set FINEX_AUTH_SERVICE_PORT=8083
  echo set FINEX_AUTH_SERVICE_URL=http://localhost:8083
  echo set FINEX_GATEWAY_PROXY_TARGET=http://localhost:8082
) > "%SHADOW_ENV_PATH%"

(
  echo @echo off
  echo setlocal EnableExtensions
  echo for %%%%I in ^("%%~dp0."^) do set "ROOT=%%%%~fI"
  echo set "RUN_DIR=%%ROOT%%\target\run"
  echo set "SOURCE_ROOT=%%ROOT:~0,-7%%"
  echo.
  echo if exist "%%ROOT%%\backend\.env.local.cmd" ^(
  echo   call :load_env_file "%%ROOT%%\backend\.env.local.cmd"
  echo   echo [finex] Loaded backend\.env.local.cmd
  echo ^) else ^(
  echo   echo [finex] No backend\.env.local.cmd found. Using current environment variables.
  echo ^)
  echo.
  echo if exist "%%ROOT%%\backend\.env.shadow.cmd" ^(
  echo   call :load_env_file "%%ROOT%%\backend\.env.shadow.cmd"
  echo   echo [finex] Loaded backend\.env.shadow.cmd
  echo ^)
  echo.
  echo if not defined FINEX_DB_URL set "FINEX_DB_URL=jdbc:mysql://localhost:3306/finex_db?useUnicode=true^&characterEncoding=utf-8^&useSSL=false^&serverTimezone=Asia/Shanghai"
  echo if not defined FINEX_DB_USERNAME set "FINEX_DB_USERNAME=root"
  echo if not defined FINEX_DB_PASSWORD set "FINEX_DB_PASSWORD=123456"
  echo if not defined FINEX_AUTH_SERVICE_PORT set "FINEX_AUTH_SERVICE_PORT=8083"
  echo if not defined FINEX_GATEWAY_PORT set "FINEX_GATEWAY_PORT=8082"
  echo if not defined FINEX_AUTH_SERVICE_URL set "FINEX_AUTH_SERVICE_URL=http://localhost:%%FINEX_AUTH_SERVICE_PORT%%"
  echo if not defined FINEX_FRONTEND_PORT set "FINEX_FRONTEND_PORT=5174"
  echo if not defined FINEX_GATEWAY_PROXY_TARGET set "FINEX_GATEWAY_PROXY_TARGET=http://localhost:%%FINEX_GATEWAY_PORT%%"
  echo if not defined FINEX_EXPENSE_ATTACHMENT_STORAGE_PATH set "FINEX_EXPENSE_ATTACHMENT_STORAGE_PATH=%%SOURCE_ROOT%%\backend\auth-service\storage\expense-attachments"
  echo.
  echo if not defined FINEX_JWT_SECRET ^(
  echo   echo [finex] FINEX_JWT_SECRET is not set. auth-service will use a temporary in-memory secret for this run.
  echo ^)
  echo.
  echo if not exist "%%ROOT%%\frontend\admin-web\node_modules\.bin\vite.cmd" ^(
  echo   echo [finex] Missing frontend dependencies in %%ROOT%%\frontend\admin-web
  echo   echo [finex] Run "cd /d %%ROOT%%\frontend\admin-web ^&^& npm install" once, then retry.
  echo   endlocal
  echo   exit /b 1
  echo ^)
  echo.
  echo echo [finex] auth-service port: %%FINEX_AUTH_SERVICE_PORT%%
  echo echo [finex] gateway port: %%FINEX_GATEWAY_PORT%%
  echo echo [finex] admin-web port: %%FINEX_FRONTEND_PORT%%
  echo echo [finex] gateway proxy target: %%FINEX_GATEWAY_PROXY_TARGET%%
  echo echo [finex] attachment storage: %%FINEX_EXPENSE_ATTACHMENT_STORAGE_PATH%%
  echo.
  echo if not exist "%%RUN_DIR%%" ^(
  echo   mkdir "%%RUN_DIR%%"
  echo ^)
  echo.
  echo echo [finex] Compiling backend/common for local classpath consistency...
  echo call mvn -f "%%ROOT%%\backend\common\pom.xml" -DskipTests compile
  echo if errorlevel 1 ^(
  echo   echo [finex] Failed to compile backend/common
  echo   endlocal
  echo   exit /b 1
  echo ^)
  echo.
  echo echo [finex] Preparing safe restart...
  echo powershell -NoProfile -ExecutionPolicy Bypass -File "%%ROOT%%\start-finex.ps1" -RootPath "%%ROOT%%" -RunDir "%%RUN_DIR%%"
  echo if errorlevel 1 ^(
  echo   echo [finex] Start aborted.
  echo   endlocal
  echo   exit /b 1
  echo ^)
  echo.
  echo endlocal
  echo exit /b 0
  echo.
  echo :load_env_file
  echo for /f "usebackq eol=; delims=" %%%%L in ^("%%~1"^) do call :apply_env_line "%%%%L"
  echo exit /b 0
  echo.
  echo :apply_env_line
  echo set "FINEX_ENV_LINE=%%~1"
  echo if not defined FINEX_ENV_LINE exit /b 0
  echo for /f "tokens=* delims= " %%%%A in ^("%%FINEX_ENV_LINE%%"^) do set "FINEX_ENV_LINE=%%%%A"
  echo if /I not "%%FINEX_ENV_LINE:~0,4%%"=="set " exit /b 0
  echo for /f "tokens=1* delims==" %%%%A in ^("%%FINEX_ENV_LINE:~4%%"^) do if not "%%%%~A"=="" set "%%%%~A=%%%%~B"
  echo set "FINEX_ENV_LINE="
  echo exit /b 0
) > "%SHADOW_START_PATH%"

(
  echo @echo off
  echo echo [shadow] Please run sync-shadow.bat from the main workspace, not from the shadow workspace.
) > "%SHADOW_SYNC_HINT_PATH%"

call :sync_frontend_dependencies
if errorlevel 1 (
  exit /b 1
)

echo [shadow] Refreshed %SHADOW_ENV_PATH%
echo [shadow] Refreshed %SHADOW_START_PATH%
echo [shadow] Refreshed %SHADOW_SYNC_HINT_PATH%
echo [shadow] Shadow workspace is ready. Run start-finex.bat inside the shadow directory when needed.
exit /b 0

:sync_frontend_dependencies
if not exist "%SOURCE_FRONTEND_DIR%\package.json" (
  exit /b 0
)

if not exist "%SHADOW_FRONTEND_DIR%\package.json" (
  echo [shadow] Missing shadow frontend package.json after sync.
  exit /b 1
)

set "CURRENT_DEPS_HASH="
for /f "usebackq delims=" %%H in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$pkg = Get-FileHash -Algorithm SHA256 '%SOURCE_FRONTEND_DIR%\package.json'; if (Test-Path '%SOURCE_FRONTEND_DIR%\package-lock.json') { $lock = Get-FileHash -Algorithm SHA256 '%SOURCE_FRONTEND_DIR%\package-lock.json'; Write-Output ($pkg.Hash + '_' + $lock.Hash) } else { Write-Output $pkg.Hash }"`) do set "CURRENT_DEPS_HASH=%%H"

if not defined CURRENT_DEPS_HASH (
  echo [shadow] Failed to calculate frontend dependency hash.
  exit /b 1
)

set "INSTALLED_DEPS_HASH="
if exist "%SHADOW_FRONTEND_STAMP_PATH%" (
  set /p INSTALLED_DEPS_HASH=<"%SHADOW_FRONTEND_STAMP_PATH%"
)

set "SHADOW_FRONTEND_NEEDS_INSTALL="
if not exist "%SHADOW_FRONTEND_DIR%\node_modules\.bin\vite.cmd" (
  set "SHADOW_FRONTEND_NEEDS_INSTALL=1"
)

if not "%CURRENT_DEPS_HASH%"=="%INSTALLED_DEPS_HASH%" (
  set "SHADOW_FRONTEND_NEEDS_INSTALL=1"
)

if not defined SHADOW_FRONTEND_NEEDS_INSTALL (
  echo [shadow] Shadow frontend dependencies are up to date.
  exit /b 0
)

echo [shadow] Installing shadow frontend dependencies...
pushd "%SHADOW_FRONTEND_DIR%" >nul
call npm install
set "SHADOW_NPM_EXIT=%ERRORLEVEL%"
popd >nul

if not "%SHADOW_NPM_EXIT%"=="0" (
  echo [shadow] npm install failed in %SHADOW_FRONTEND_DIR%
  exit /b %SHADOW_NPM_EXIT%
)

if not exist "%SHADOW_FRONTEND_DIR%\node_modules\.bin\vite.cmd" (
  echo [shadow] Frontend dependencies finished installing, but vite.cmd is still missing.
  exit /b 1
)

> "%SHADOW_FRONTEND_STAMP_PATH%" echo %CURRENT_DEPS_HASH%
echo [shadow] Shadow frontend dependencies are ready.
exit /b 0
