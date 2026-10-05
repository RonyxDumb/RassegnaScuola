# Verifica della consegna

Base: upstream `1f479713f69f65b516649fdf8d75551c0411b8b1`.
Versione: 3.0.0, Android versionCode 6.

Comando eseguito con JDK 17 e SDK Android 35:

```sh
gradle :shared:verifyCore :windows:installDist :android:app:assembleDebug :android:app:lintDebug
```

Esito: **BUILD SUCCESSFUL**.

- 25 controlli condivisi superati: RSS/Atom, date e fusi, esclusione di notizie vecchie/senza data/future, duplicati, limite di cinque, XML dei documenti Word, versioni e asset Android/Windows.
- APK generato e firma debug verificata con `apksigner`. Identificativo Android e versioni controllati con `aapt`.
- Lint Android: nessun errore; rimangono avvisi di localizzazione dei testi italiani, compatibilità delle risorse e forma delle icone legacy.
- Codice desktop compilato; interfaccia avviata in ambiente grafico Linux e controllata a 1100×820 e 780×650, con contenuti di prova esplicitamente dimostrativi. Verificato il ritorno a capo dei titoli lunghi.
- Packaging `jpackage app-image` provato su Linux: launcher configurato con i JAR condivisi, Gson e FlatLaf, runtime incluso. Il pacchetto EXE Windows non è stato prodotto o eseguito in questo ambiente Linux; viene generato dallo script PowerShell su Windows o dal job Windows di GitHub Actions.
- Workflow YAML analizzato; la GitHub Action non è stata eseguita sul repository remoto.

Nello ZIP è incluso `output/RassegnaScuola-debug.apk`. È un APK di prova: non pubblicarlo come release firmata di produzione. L'app Android non è stata avviata su un telefono o emulatore in questa verifica. Il controllo aggiornamenti è verificato con fixture di release; il download/installazione effettivo di una nuova versione non è stato eseguito.

## Correzione script Windows (Java 25 / major version 69)

Il controllo del JDK nello script precedente era insufficiente: accettava anche Java 25. Lo script aggiornato verifica JDK 17 e architettura x64, imposta JAVA_HOME, PATH e org.gradle.java.home, oppure prepara Temurin 17 portatile per il progetto.

- 13 controlli PowerShell di parsing e selezione superati: JDK 17, esclusione di Java 25/170, esclusione di JRE e JDK senza jpackage, rifiuto di ARM64, percorsi con spazi, scelta del 17 quando JAVA_HOME indica il 25, override esplicito.
- Download reale del pacchetto Temurin 17 Windows x64, verifica SHA256, estrazione e controllo versione/architettura completati.
- I controlli sono stati eseguiti con PowerShell su Linux; non equivalgono all’esecuzione dell’EXE o alla compilazione completa sul PC Windows.

Per applicare questa correzione a un progetto gia estratto e sufficiente sostituire la cartella scripts/ e rilanciare build_windows.bat.
