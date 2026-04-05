@echo off
setlocal EnableExtensions

set "ROOT=%~dp0"

if exist "%ROOT%backend\.env.local.cmd" (
  call "%ROOT%backend\.env.local.cmd"
  echo [finex] Loaded backend\.env.local.cmd
) else (
  echo [finex] No backend\.env.local.cmd found. Using current environment variables.
)

if exist "%ROOT%backend\.env.shadow.cmd" (
  call "%ROOT%backend\.env.shadow.cmd"
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

echo [finex] Compiling backend/common for local classpath consistency...
call mvn -f "%ROOT%backend\common\pom.xml" -DskipTests compile
if errorlevel 1 (
  echo [finex] Failed to compile backend/common
  endlocal
  exit /b 1
)

echo [finex] Checking occupied ports...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ports = @(%FINEX_FRONTEND_PORT%, %FINEX_GATEWAY_PORT%, %FINEX_AUTH_SERVICE_PORT%) | Sort-Object -Unique; " ^
  "$connections = Get-NetTCPConnection -LocalPort $ports -State Listen -ErrorAction SilentlyContinue; " ^
  "if ($connections) { " ^
  "  $connections | Group-Object OwningProcess | ForEach-Object { " ^
  "    $portsForProcess = ($_.Group | Select-Object -ExpandProperty LocalPort -Unique | Sort-Object) -join ', '; " ^
  "    try { Stop-Process -Id [int]$_.Name -Force -ErrorAction Stop; Write-Host ('[finex] Stopped process ' + $_.Name + ' on ports ' + $portsForProcess) } " ^
  "    catch { Write-Host ('[finex] Failed to stop process ' + $_.Name + ' on ports ' + $portsForProcess + ': ' + $_.Exception.Message) } " ^
  "  } " ^
  "} else { Write-Host ('[finex] Ports ' + ($ports -join ', ') + ' are free.') }"

start "finex-auth-service" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%backend\auth-service'; mvn spring-boot:run"
start "finex-gateway" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%backend\gateway'; mvn spring-boot:run"
start "finex-admin-web" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%frontend\admin-web'; npm run dev"

endlocal
