@echo off
setlocal
cd /d "%~dp0"
docker info >nul 2>&1
if errorlevel 1 (
  echo Avvia Docker Desktop con motore Linux e WSL 2.
  pause
  exit /b 1
)
docker build -f Dockerfile.android -t rassegnascuola-android .
if errorlevel 1 goto fail
if not exist output mkdir output
docker run --rm -v "%cd%:/project" -v rassegna-gradle:/root/.gradle -v rassegna-android-keys:/root/.android rassegnascuola-android sh -c "gradle :shared:verifyCore :android:app:assembleDebug --no-daemon && cp android/app/build/outputs/apk/debug/app-debug.apk output/RassegnaScuola-debug.apk"
if errorlevel 1 goto fail
echo Creato: output\RassegnaScuola-debug.apk
pause
exit /b 0
:fail
echo Compilazione Android fallita. Leggi l'errore sopra.
pause
exit /b 1
