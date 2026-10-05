@echo off
setlocal
cd /d "%~dp0"
echo [1/2] Windows
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build_windows.ps1"
if errorlevel 1 goto fail
echo [2/2] Android
call docker_build.bat
if errorlevel 1 goto fail
echo Entrambi i target sono disponibili in output.
exit /b 0
:fail
echo Procedura interrotta: correggi l'errore e riprova.
pause
exit /b 1
