@echo off
setlocal

set "ROOT=%~dp0"

if exist "%ROOT%backend\.env.local.cmd" (
  call "%ROOT%backend\.env.local.cmd"
  echo [finex] Loaded backend\.env.local.cmd
) else (
  echo [finex] No backend\.env.local.cmd found. Using current environment variables.
)

if not defined FINEX_DB_URL set "FINEX_DB_URL=jdbc:mysql://localhost:3306/finex_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai"
if not defined FINEX_DB_USERNAME set "FINEX_DB_USERNAME=root"
if not defined FINEX_DB_PASSWORD set "FINEX_DB_PASSWORD=123456"

if not defined FINEX_JWT_SECRET (
  echo [finex] FINEX_JWT_SECRET is not set. auth-service will use a temporary in-memory secret for this run.
)

echo [finex] Compiling backend/common for local classpath consistency...
call mvn -f "%ROOT%backend\common\pom.xml" -DskipTests compile
if errorlevel 1 (
  echo [finex] Failed to compile backend/common
  endlocal
  exit /b 1
)

echo [finex] Checking occupied backend ports...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ports = 8080,8081; " ^
  "$connections = Get-NetTCPConnection -LocalPort $ports -State Listen -ErrorAction SilentlyContinue; " ^
  "if ($connections) { " ^
  "  $connections | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { " ^
  "    try { Stop-Process -Id $_ -Force -ErrorAction Stop; Write-Host ('[finex] Stopped process ' + $_) } " ^
  "    catch { Write-Host ('[finex] Failed to stop process ' + $_ + ': ' + $_.Exception.Message) } " ^
  "  } " ^
  "} else { Write-Host '[finex] Ports 8080/8081 are free.' }"

start "finex-auth-service" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%backend\auth-service'; mvn spring-boot:run"
start "finex-gateway" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%backend\gateway'; mvn spring-boot:run"
start "finex-admin-web" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%frontend\admin-web'; npm run dev"

endlocal
