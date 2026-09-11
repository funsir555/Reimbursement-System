@echo off
setlocal
chcp 65001 >nul
set "PS7_EXE=%LocalAppData%\Programs\PowerShell\7\pwsh.exe"
if exist "%PS7_EXE%" (
  "%PS7_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%~dp0export_full_database_to_excel_interactive.ps1"
) else (
  "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0export_full_database_to_excel_interactive.ps1"
)
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo Export launcher failed. Exit code: %EXIT_CODE%
)
pause
exit /b %EXIT_CODE%
