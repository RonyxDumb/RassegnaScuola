# Functions only: dot-source this file before starting Gradle or jpackage.
function Test-RsJdk17 {
    param([string]$JdkRoot)
    if ([string]::IsNullOrWhiteSpace($JdkRoot)) { return $false }
    foreach ($binary in @('java.exe', 'javac.exe', 'jpackage.exe')) {
        if (!(Test-Path -LiteralPath (Join-Path $JdkRoot "bin/$binary") -PathType Leaf)) { return $false }
    }
    $release = Join-Path $JdkRoot 'release'
    if (!(Test-Path -LiteralPath $release -PathType Leaf)) { return $false }
    $metadata = Get-Content -LiteralPath $release -Raw
    return ($metadata -match '(?m)^JAVA_VERSION="17(?:\.|"|\+)') -and
           ($metadata -match '(?m)^OS_ARCH="(?:amd64|x86_64)"')
}
function Find-RsJdk17 {
    param([string[]]$Candidates)
    foreach ($candidate in $Candidates) {
        if (Test-RsJdk17 -JdkRoot $candidate) { return (Get-Item -LiteralPath $candidate).FullName }
    }
    return $null
}
function Get-RsJdk17 {
    param([Parameter(Mandatory=$true)][string]$ProjectRoot)
    if ($env:RS_JDK17) {
        if (!(Test-RsJdk17 -JdkRoot $env:RS_JDK17)) { throw 'RS_JDK17 deve indicare un JDK 17 x64 completo (non Java 25 o un JRE).' }
        return (Get-Item -LiteralPath $env:RS_JDK17).FullName
    }
    $cache = Join-Path $ProjectRoot '.build-tools/jdk17'
    $candidates = @($cache, $env:JAVA_HOME_17_X64, $env:JAVA_HOME)
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $candidates += Split-Path (Split-Path $compiler.Source -Parent) -Parent }
    foreach ($base in @($env:ProgramFiles, $env:LOCALAPPDATA)) {
        if (!$base) { continue }
        foreach ($vendor in @('Eclipse Adoptium', 'Microsoft', 'Java', 'OpenJDK', 'Amazon Corretto', 'Programs/Eclipse Adoptium')) {
            $parent = Join-Path $base $vendor
            if (Test-Path -LiteralPath $parent -PathType Container) {
                $candidates += @(Get-ChildItem -LiteralPath $parent -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
            }
        }
    }
    $installed = Find-RsJdk17 -Candidates $candidates
    if ($installed) { return $installed }

    Write-Host 'JDK 17 x64 non trovato. Scarico Temurin 17 portatile per questo progetto...' -ForegroundColor Cyan
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $ProgressPreference = 'SilentlyContinue'
    $api = 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
    $assets = @(Invoke-RestMethod -Uri $api -TimeoutSec 60)
    $package = $assets[0].binary.package
    if (!$package -or $package.checksum -notmatch '^[0-9a-fA-F]{64}$') { throw 'Risposta Adoptium non valida.' }
    $cacheParent = Join-Path $ProjectRoot '.build-tools'
    New-Item -ItemType Directory -Path $cacheParent -Force | Out-Null
    $temporary = Join-Path $cacheParent ('download-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $temporary | Out-Null
    try {
        $archive = Join-Path $temporary 'jdk17.zip'
        Invoke-WebRequest -UseBasicParsing -Uri $package.link -OutFile $archive -TimeoutSec 600
        if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $package.checksum) { throw 'SHA256 JDK non corrispondente. Riprova il download.' }
        $extract = Join-Path $temporary 'extracted'
        Expand-Archive -LiteralPath $archive -DestinationPath $extract
        $unpacked = Find-RsJdk17 -Candidates @(Get-ChildItem -LiteralPath $extract -Directory | Select-Object -ExpandProperty FullName)
        if (!$unpacked) { throw 'Il pacchetto scaricato non contiene un JDK 17 x64 completo.' }
        if (Test-Path -LiteralPath $cache) { Remove-Item -LiteralPath $cache -Recurse -Force }
        Move-Item -LiteralPath $unpacked -Destination $cache
    } catch {
        throw ('Preparazione JDK 17 non riuscita: ' + $_.Exception.Message + ' Installa Temurin JDK 17 x64 oppure imposta RS_JDK17 al suo percorso.')
    } finally {
        Remove-Item -LiteralPath $temporary -Recurse -Force -ErrorAction SilentlyContinue
    }
    return $cache
}
