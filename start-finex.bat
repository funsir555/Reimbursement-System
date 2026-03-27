@echo off
setlocal

set "ROOT=%~dp0"

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
