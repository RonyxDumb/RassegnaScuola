<p align="center"><img src="assets/icon.png" width="100" alt="Rassegna Scuola"></p>
<h1 align="center">Rassegna Scuola</h1>
<p align="center">Android · Windows<br>Francesco Pio Pipino</p>

Un argomento, cinque notizie di oggi e un documento Word. Un solo repository contiene entrambe le applicazioni: il motore delle notizie, l'esportazione e il controllo aggiornamenti sono condivisi.

| Android | Windows |
| --- | --- |
| Android 10 o successivo | Windows 10/11, x64 |
| Barre di sistema con sfondo opaco fisso, anche in navigazione a gesti | Finestra ridimensionabile con barra laterale |
| Griglia degli argomenti, schede leggibili, condivisione Word | Schede delle notizie, apertura Word e cartella |
| Icona adattiva e monocromatica per icone a tema | Icona ICO e launcher EXE con Java incluso |

Palette ardesia e lavanda, testi essenziali e spaziature ampie.

## Argomenti

Economia · Politica · Geo Politica · Scienze e tecnologia · Sport · Cronaca · Attualità.

Prima di creare la rassegna occorre scegliere l'argomento. Le notizie provengono dai feed ANSA e da ricerche Google News su testate selezionate. Non occorrono account né chiavi API.

La selezione usa la data di pubblicazione nel fuso **Europe/Rome**: questo non prova che l'evento sia accaduto nello stesso giorno. I duplicati e le voci senza data vengono esclusi. Se ci sono meno di cinque risultati idonei, l'app conserva soltanto quelli disponibili. Gli estratti provengono dai feed; Google News può fornire soltanto il titolo, e in quel caso compare «Estratto non disponibile». L'app non genera riassunti con AI e non inventa contenuti mancanti. La pertinenza è una selezione euristica, non una verifica giornalistica; i collegamenti consentono di leggere il contesto. I feed possono cambiare o non essere raggiungibili.

I documenti `.docx` vengono salvati in `Download/RassegnaScuola` su Android e `%USERPROFILE%\Downloads\RassegnaScuola` su Windows. I file hanno un nome con data e ora. La versione iniziale Windows è portatile: estrarre tutta la cartella, non spostare soltanto l'EXE.

## Compilare entrambi da Windows

1. Estrai **tutto** il sorgente ZIP in una cartella locale.
2. Lo script Windows cerca un **JDK 17 x64 completo**. Se manca, scarica automaticamente Temurin 17 portatile in `.build-tools/jdk17`, verifica SHA256 e lo riusa nelle compilazioni successive. Non modifica Java installato nel sistema. Puoi anche installare [Temurin JDK 17 x64](https://adoptium.net/temurin/releases/?version=17) o indicarne il percorso con `RS_JDK17`. JDK 25 e altri JDK incompatibili vengono ignorati.
3. Installa e avvia **Docker Desktop**, con contenitori Linux/WSL 2.
4. Esegui **`build_all.bat`** dalla cartella del progetto.

Risultati in `output/`:

- `RassegnaScuola-debug.apk`
- `RassegnaScuola-3.0.0-windows-x64.zip`

Lo ZIP include anche un APK di prova già compilato in `output/`.

La prima compilazione richiede Internet e diversi GB liberi. Gradle Wrapper scarica Gradle 8.7. Docker prepara JDK 17, SDK Android 35 e Build Tools 35.0.0. Lo script imposta `JAVA_HOME` e `PATH` al JDK 17 selezionato soltanto per il processo di compilazione. Windows usa `jpackage --type app-image` e include il runtime Java: sul PC che usa l'app non serve installare Java. Non servono Visual Studio, WiX o un installer MSI.

Se vuoi compilare un target solo:

```bat
REM Android, con Docker Desktop
docker_build.bat

REM Windows, con JDK 17 x64
build_windows.bat
```

Per eseguire i BAT da PowerShell usa `cmd /c .\build_all.bat`, oppure `cmd /c .\docker_build.bat`. Se un programma tenta di aprirli come documenti, avviali dal terminale.

### Android senza Docker

Apri la **root** del progetto in Android Studio e installa SDK 35. Poi:

```bat
gradlew.bat :shared:verifyCore :android:app:assembleDebug
```

L'APK è in `android/app/build/outputs/apk/debug/app-debug.apk`. Per una build di distribuzione usa una chiave di firma tua; configurare `RS_KEYSTORE`, `RS_STORE_PASSWORD`, `RS_KEY_ALIAS`, `RS_KEY_PASSWORD`, quindi eseguire `:android:app:assembleRelease`. Senza queste variabili la release locale è **non firmata**.

### Sviluppo e controlli

```bat
gradlew.bat :shared:verifyCore
gradlew.bat :android:app:lintDebug
gradlew.bat :windows:run
```

## Controllo aggiornamenti

Entrambe le applicazioni verificano all'avvio, al massimo una volta ogni 24 ore, la release stabile più recente su:

**[RonyxDumb/RassegnaScuola-Android](https://github.com/RonyxDumb/RassegnaScuola-Android/releases)**

Da **Impostazioni** puoi disattivare il controllo automatico oppure scegliere **Verifica ora**. Gli errori di rete non impediscono l'uso dell'app. Nessuna release pubblicata viene distinta da una versione già aggiornata.

I tag devono essere numerici, per esempio `v3.0.1`. Sono ignorate le prerelease. L'app confronta i numeri della versione e propone l'asset del target; se non lo trova, apre la pagina della release. Il download si apre nel browser e l'installazione/sostituzione rimane esplicita:

- Android: installa il nuovo APK firmato con la stessa chiave.
- Windows: chiudi l'app, estrai lo ZIP nuovo e usa la nuova cartella completa. Le impostazioni rimangono nelle preferenze utente.

Il controllo non installa automaticamente file e non richiede permessi per installare pacchetti. I link di aggiornamento vengono accettati solo dalle release del repository configurato. Non sono ancora disponibili installer automatici o aggiornamenti differenziali.

### Pubblicare nuove versioni da un solo caricamento

1. Carica questa struttura nello stesso repository, mantenendo `android/`, `windows/`, `shared/` e `.github/` alla root. Se stai sostituendo l'upstream, rimuovi la vecchia directory `app/` alla root: ora si trova in `android/app/`. Non caricare `output/`, cache, password o keystore.
2. Modifica **soltanto `app-version.properties`**: `versionName` per entrambi e `versionCode` Android, sempre crescente. Il repository del controllo aggiornamenti è definito nello stesso file; se rinomini il repo, aggiorna `updateRepository` prima di distribuire la nuova versione.
3. Per le release automatiche configura in GitHub → Settings → Secrets and variables → Actions:
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_STORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
4. Crea e carica il tag corrispondente, ad esempio `v3.0.1` se `versionName=3.0.1`.

Il workflow compila e verifica Android su Linux e Windows su Windows. Su `main` e pull request produce artefatti di test; sul tag pubblica una release con APK firmato e ZIP Windows. Se manca la firma Android, la pubblicazione si ferma. La versione del tag deve corrispondere a quella nel file properties.

Gli asset attesi sono:

```text
RassegnaScuola-3.0.1-android.apk
RassegnaScuola-3.0.1-windows-x64.zip
```

Puoi anche caricarli manualmente in una release stabile. I file con `debug` nel nome non vengono scelti come aggiornamenti Android.

**Conserva la chiave Android e i suoi backup.** La compilazione Docker di test mantiene la chiave debug nel volume locale `rassegna-android-keys`, ma quella chiave non è adatta alle release pubbliche. Se l'app già installata è stata firmata con una chiave differente, il nuovo APK non può aggiornarla in-place: usa la stessa chiave originale oppure disinstalla la versione precedente.

## Struttura

```text
android/app/       Interfaccia Android, barre opache, salvataggio MediaStore
windows/           Interfaccia desktop Java/FlatLaf, launcher Windows
shared/            Sette argomenti, feed, selezione, Word, aggiornamenti
assets/            Icona SVG, PNG e ICO
scripts/           Packaging Windows
.github/workflows/ Build e release dei due target
app-version.properties  Versione e repository aggiornamenti
build_all.bat      Compila entrambi
docker_build.bat   Compila Android
build_windows.bat  Compila Windows
```

## Origine

Derivato dall'upstream Android al commit `1f479713f69f65b516649fdf8d75551c0411b8b1`. Mantiene l'identificativo Android `com.ronyxdumb.rassegnascuola`, la selezione dei sette argomenti e l'esportazione Word. Versione monorepo: **3.0.0**, Android `versionCode=6` (upstream: 5). Icona vettoriale ridisegnata con un giornale a due fogli; sorgente modificabile in `assets/icon.svg`.

La verifica eseguita per questa consegna è descritta in [docs/VERIFICA.md](docs/VERIFICA.md).

## Correzione build Windows: major version 69

`Unsupported class file major version 69` indica bytecode Java 25. Gradle 8.7 non supporta l’esecuzione su Java 25. La prima versione dello script accettava qualsiasi JDK nel `JAVA_HOME`: questa revisione verifica la versione e l’architettura, seleziona JDK 17 x64, oppure lo prepara automaticamente. Gli avvisi `System::load` non sono l’errore che blocca la compilazione. Non occorre rimuovere Java 25.

Per aggiornare il progetto esistente sostituisci la cartella `scripts/` con quella di questo ZIP, quindi rilancia `build_windows.bat`. Entrambi i file `build_windows.ps1` e `jdk17.ps1` sono necessari. Anche `build_all.bat` usa la procedura corretta.

Per scegliere manualmente un JDK in PowerShell, usa il percorso reale della tua installazione:

```powershell
$env:RS_JDK17 = 'C:\percorso\del\jdk-17'
cmd /c .\build_windows.bat
```
