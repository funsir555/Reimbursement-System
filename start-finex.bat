@echo off
setlocal EnableExtensions

for %%I in ("%~dp0.") do set "ROOT=%%~fI"
set "RUN_DIR=%ROOT%\target\run"
set "ENV_LOCAL=%ROOT%\backend\.env.local.cmd"
set "ENV_SHADOW=%ROOT%\backend\.env.shadow.cmd"
set "POWERSHELL_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "START_SCRIPT=%ROOT%\start-finex.ps1"

call :log "FinEx one-click startup entry"
call :log "Repository root: %ROOT%"

call :load_optional_env "%ENV_LOCAL%" "backend\.env.local.cmd"
call :load_optional_env "%ENV_SHADOW%" "backend\.env.shadow.cmd"

call :apply_default FINEX_DB_URL "jdbc:mysql://localhost:3306/finex_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai"
call :apply_default FINEX_DB_USERNAME "root"
call :apply_default FINEX_AUTH_SERVICE_PORT "8081"
call :apply_default FINEX_GATEWAY_PORT "8080"
call :apply_default FINEX_FRONTEND_PORT "5173"
call :apply_default FINEX_AUTH_SERVICE_URL "http://localhost:%FINEX_AUTH_SERVICE_PORT%"
call :apply_default FINEX_GATEWAY_PROXY_TARGET "http://localhost:%FINEX_GATEWAY_PORT%"

call :require_env FINEX_DB_PASSWORD "replace-with-your-db-password" "set FINEX_DB_PASSWORD=your-real-db-password"
if errorlevel 1 goto :failed

call :require_env FINEX_JWT_SECRET "replace-with-a-long-random-secret-at-least-32-chars" "set FINEX_JWT_SECRET=your-long-random-secret-at-least-32-chars"
if errorlevel 1 goto :failed

if "%FINEX_JWT_SECRET:~31,1%"=="" (
  call :log "FINEX_JWT_SECRET must be at least 32 characters long."
  call :print_env_hint "set FINEX_JWT_SECRET=your-long-random-secret-at-least-32-chars"
  goto :failed
)

if not exist "%RUN_DIR%" (
  mkdir "%RUN_DIR%" >nul 2>nul
  if errorlevel 1 (
    call :log "Failed to create runtime directory: %RUN_DIR%"
    goto :failed
  )
)

call :print_runtime_summary

call :log "Compiling backend/common before startup..."
call mvn -f "%ROOT%\backend\common\pom.xml" -DskipTests compile
if errorlevel 1 (
  call :log "backend/common compilation failed. FinEx was not started."
  goto :failed
)

call :log "Delegating process and port management to start-finex.ps1 ..."
"%POWERSHELL_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%START_SCRIPT%" -RootPath "%ROOT%" -RunDir "%RUN_DIR%"
if errorlevel 1 (
  call :log "start-finex.ps1 reported a startup failure. Review the conflict details above and retry."
  goto :failed
)

call :log "FinEx startup command dispatched successfully."
call :log "Auth service:   http://localhost:%FINEX_AUTH_SERVICE_PORT%"
call :log "Gateway:        http://localhost:%FINEX_GATEWAY_PORT%"
call :log "Admin web:      http://localhost:%FINEX_FRONTEND_PORT%"
endlocal
exit /b 0

:load_optional_env
if exist "%~1" (
  call :load_env_file "%~1"
  call :log "Loaded %~2"
) else (
  call :log "Optional env file not found: %~2"
)
exit /b 0

:apply_default
call set "FINEX_CURRENT_VALUE=%%%~1%%"
if not defined FINEX_CURRENT_VALUE set "%~1=%~2"
set "FINEX_CURRENT_VALUE="
exit /b 0

:load_env_file
for /f "usebackq eol=; delims=" %%L in ("%~1") do call :apply_env_line "%%L"
exit /b 0

:apply_env_line
set "FINEX_ENV_LINE=%~1"
if not defined FINEX_ENV_LINE exit /b 0
for /f "tokens=* delims= " %%A in ("%FINEX_ENV_LINE%") do set "FINEX_ENV_LINE=%%A"
if not defined FINEX_ENV_LINE exit /b 0
if "%FINEX_ENV_LINE:~0,1%"=="#" goto :clear_env_line
if /I "%FINEX_ENV_LINE:~0,3%"=="rem" goto :clear_env_line
if /I not "%FINEX_ENV_LINE:~0,4%"=="set " goto :clear_env_line
for /f "tokens=1* delims==" %%A in ("%FINEX_ENV_LINE:~4%") do if not "%%~A"=="" set "%%~A=%%~B"
:clear_env_line
set "FINEX_ENV_LINE="
exit /b 0

:require_env
set "FINEX_REQUIRED_NAME=%~1"
set "FINEX_REQUIRED_PLACEHOLDER=%~2"
set "FINEX_REQUIRED_EXAMPLE=%~3"
call set "FINEX_REQUIRED_VALUE=%%%FINEX_REQUIRED_NAME%%%"

if not defined FINEX_REQUIRED_VALUE (
  call :log "%FINEX_REQUIRED_NAME% is required. Set it in backend\.env.local.cmd or the current shell before running start-finex.bat."
  call :print_env_hint "%FINEX_REQUIRED_EXAMPLE%"
  goto :require_env_failed
)

if /I "%FINEX_REQUIRED_VALUE%"=="%FINEX_REQUIRED_PLACEHOLDER%" (
  call :log "%FINEX_REQUIRED_NAME% is still using the example placeholder. Replace it before starting FinEx."
  call :print_env_hint "%FINEX_REQUIRED_EXAMPLE%"
  goto :require_env_failed
)

goto :require_env_success

:require_env_failed
set "FINEX_REQUIRED_NAME="
set "FINEX_REQUIRED_PLACEHOLDER="
set "FINEX_REQUIRED_EXAMPLE="
set "FINEX_REQUIRED_VALUE="
exit /b 1

:require_env_success
set "FINEX_REQUIRED_NAME="
set "FINEX_REQUIRED_PLACEHOLDER="
set "FINEX_REQUIRED_EXAMPLE="
set "FINEX_REQUIRED_VALUE="
exit /b 0

:print_runtime_summary
call :log "Runtime ports and targets:"
call :log "  auth-service         : http://localhost:%FINEX_AUTH_SERVICE_PORT%"
call :log "  gateway              : http://localhost:%FINEX_GATEWAY_PORT%"
call :log "  admin-web            : http://localhost:%FINEX_FRONTEND_PORT%"
call :log "  auth-service upstream: %FINEX_AUTH_SERVICE_URL%"
call :log "  gateway proxy target : %FINEX_GATEWAY_PROXY_TARGET%"
exit /b 0

:print_env_hint
call :log "Example backend\.env.local.cmd line: %~1"
exit /b 0

:failed
call :log "Startup aborted."
endlocal
exit /b 1

:log
echo [finex] %~1
exit /b 0

