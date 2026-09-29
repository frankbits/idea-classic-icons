# Classic Icons (New UI)

JetBrains-Plugin (IntelliJ Platform, ab Build 243), das in der New UI die alten Icons verwendet,
ohne wie ClassicUI die gesamte New UI abzuschalten.

Der `IconPathPatcher` entfernt das Theme-Patcher der New UI aushebelt (Originalpfad zurückgeben, wenn ein `expui/`-Gegenstück existiert).
Einstellung: **Settings | Appearance & Behavior | Classic Icons**

- All icons
- Only files and folders (`fileTypes/`, `nodes/` – siehe `ClassicIconPatcher.FILES_AND_FOLDERS`)

## Bauen

```
gradle wrapper        # einmalig, falls gradlew fehlt
./gradlew runIde      # Test-IDE starten
./gradlew verifyPlugin
./gradlew buildPlugin # ZIP in build/distributions/
```

Benötigt JDK 21.

## Struktur

```
classic-icons/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── src/main/
    ├── kotlin/dev/frankbits/classicicons/
    │   ├── ClassicIconPatcher.kt        (Patcher + Startup-Component)
    │   ├── ClassicIconsSettings.kt      (persistente Einstellung)
    │   └── ClassicIconsConfigurable.kt  (Settings-UI)
    └── resources/META-INF/plugin.xml
```
