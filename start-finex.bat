@echo off
setlocal EnableExtensions

for %%I in ("%~dp0.") do set "ROOT=%%~fI"
set "RUN_DIR=%ROOT%\target\run"

if exist "%ROOT%\backend\.env.local.cmd" (
  call :load_env_file "%ROOT%\backend\.env.local.cmd"
  echo [finex] Loaded backend\.env.local.cmd
) else (
  echo [finex] No backend\.env.local.cmd found. Using current environment variables.
)

if exist "%ROOT%\backend\.env.shadow.cmd" (
  call :load_env_file "%ROOT%\backend\.env.shadow.cmd"
  echo [finex] Loaded backend\.env.shadow.cmd
)

if not defined FINEX_DB_URL set "FINEX_DB_URL=jdbc:mysql://localhost:3306/finex_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai"
if not defined FINEX_DB_USERNAME set "FINEX_DB_USERNAME=root"
if not defined FINEX_DB_PASSWORD set "FINEX_DB_PASSWORD=123456"

if not defined FINEX_AUTH_SERVICE_PORT set "FINEX_AUTH_SERVICE_PORT=8081"
if not defined FINEX_GATEWAY_PORT set "FINEX_GATEWAY_PORT=8080"
if not defined FINEX_AUTH_SERVICE_URL set "FINEX_AUTH_SERVICE_URL=http://localhost:%FINEX_AUTH_SERVICE_PORT%"
if not defined FINEX_FRONTEND_PORT set "FINEX_FRONTEND_PORT=5173"
if not defined FINEX_GATEWAY_PROXY_TARGET set "FINEX_GATEWAY_PROXY_TARGET=http://localhost:%FINEX_GATEWAY_PORT%"

if not defined FINEX_JWT_SECRET (
  echo [finex] FINEX_JWT_SECRET is not set. auth-service will use a temporary in-memory secret for this run.
)

echo [finex] auth-service port: %FINEX_AUTH_SERVICE_PORT%
echo [finex] gateway port: %FINEX_GATEWAY_PORT%
echo [finex] admin-web port: %FINEX_FRONTEND_PORT%
echo [finex] gateway proxy target: %FINEX_GATEWAY_PROXY_TARGET%

if not exist "%RUN_DIR%" (
  mkdir "%RUN_DIR%"
)

echo [finex] Compiling backend/common for local classpath consistency...
call mvn -f "%ROOT%\backend\common\pom.xml" -DskipTests compile
if errorlevel 1 (
  echo [finex] Failed to compile backend/common
  endlocal
  exit /b 1
)

echo [finex] Preparing safe restart...
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%\start-finex.ps1" -RootPath "%ROOT%" -RunDir "%RUN_DIR%"
if errorlevel 1 (
  echo [finex] Start aborted.
  endlocal
  exit /b 1
)

endlocal
exit /b 0

:load_env_file
for /f "usebackq eol=; delims=" %%L in ("%~1") do call :apply_env_line "%%L"
exit /b 0

:apply_env_line
set "FINEX_ENV_LINE=%~1"
if not defined FINEX_ENV_LINE exit /b 0
for /f "tokens=* delims= " %%A in ("%FINEX_ENV_LINE%") do set "FINEX_ENV_LINE=%%A"
if /I not "%FINEX_ENV_LINE:~0,4%"=="set " exit /b 0
for /f "tokens=1* delims==" %%A in ("%FINEX_ENV_LINE:~4%") do if not "%%~A"=="" set "%%~A=%%~B"
set "FINEX_ENV_LINE="
exit /b 0
