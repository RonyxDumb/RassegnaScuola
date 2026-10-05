$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
try {
    . (Join-Path $PSScriptRoot 'jdk17.ps1')
    $jdk = Get-RsJdk17 -ProjectRoot (Get-Location).Path
    $jpackage = Join-Path $jdk 'bin/jpackage.exe'
    $env:JAVA_HOME = $jdk
    $env:PATH = (Join-Path $jdk 'bin') + [IO.Path]::PathSeparator + $env:PATH
    Write-Host "JDK 17 x64 selezionato: $jdk" -ForegroundColor Cyan
    & (Join-Path $jdk 'bin/java.exe') -version
    if ($LASTEXITCODE -ne 0) { throw 'Avvio JDK 17 non riuscito.' }
    & .\gradlew.bat :windows:installDist --no-daemon "-Dorg.gradle.java.home=$jdk"
    if ($LASTEXITCODE -ne 0) { throw 'Compilazione Java non riuscita.' }
    $version = (Get-Content app-version.properties | Where-Object { $_ -match '^versionName=' }) -replace '^versionName=', ''
    $stage = Join-Path (Get-Location) 'windows\build\package'
    if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
    New-Item $stage -ItemType Directory -Force | Out-Null
    & $jpackage --type app-image --name RassegnaScuola --input windows\build\install\windows\lib --main-jar RassegnaScuola.jar --main-class com.ronyxdumb.rassegnascuola.WindowsApp --app-version $version --vendor 'Francesco Pio Pipino' --icon assets\icon.ico --dest $stage --add-modules java.desktop,java.xml,java.logging,java.prefs,jdk.crypto.ec --java-options '-Dfile.encoding=UTF-8'
    if ($LASTEXITCODE -ne 0) { throw 'Creazione applicazione Windows non riuscita.' }
    New-Item output -ItemType Directory -Force | Out-Null
    $zip = "output\RassegnaScuola-$version-windows-x64.zip"
    if (Test-Path $zip) { Remove-Item $zip -Force }
    Compress-Archive -Path "$stage\RassegnaScuola" -DestinationPath $zip -CompressionLevel Optimal
    Write-Host "Creato: $zip" -ForegroundColor Green
    Write-Host 'Estrai tutto lo ZIP e avvia RassegnaScuola\RassegnaScuola.exe.'
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}
