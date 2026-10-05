@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build_windows.ps1"
if errorlevel 1 goto fail
echo Compilazione Windows completata.
pause
exit /b 0
:fail
echo Compilazione Windows fallita. Leggi l'errore sopra.
pause
exit /b 1
