# Classic Icons (New UI)

JetBrains-Plugin für IntelliJ Platform ab Build 243. Das Plugin verwendet in der New UI
klassische Icon-Pfade, ohne die gesamte New UI wie ClassicUI abzuschalten.

Einstellungen befinden sich unter **Settings | Appearance & Behavior | Classic Icons**.
Die Icon-Liste wird aus zwei Quellen aufgebaut:

- **Registered icons**: FileTypeManager und ActionManager liefern bekannte Icons inklusive
  FileType-/Action-Metadaten.
- **Runtime-discovered icons**: weitere Pfade werden erst registriert, wenn IntelliJ sie
  tatsächlich anfordert. Diese Liste kann daher unvollständig sein.

Die Gruppen sind nach dem ersten Pfadsegment gegliedert und können unabhängig voneinander
auf- und zugeklappt werden. Gruppen-Checkboxen haben drei Zustände: vollständig aktiv,
vollständig deaktiviert und teilweise aktiv. Einzelne Icons können ebenfalls ausgeschlossen
werden. Eine Gruppe mit gleichem Namen in den beiden Quellen wird getrennt behandelt.

## Modi

- **Don't use classic icons (New UI)**: Der Patcher bleibt deaktiviert.
- **Classic icons for everything**: Alle bekannten klassischen Pfade werden berücksichtigt.
- **Classic icons only for files and folders**: Nur Datei-/Ordnerpfade, registrierte
  FileTypes und zusätzliche Pfadfilter werden berücksichtigt.

Ein Custom-Icon-Verzeichnis kann die Originalpfade spiegeln, z. B.
`fileTypes/java.svg` oder `icons/MarkdownPlugin.svg`. SVG- und PNG-Dateien werden unterstützt
und überschreiben die klassische Vorschau.

## Bauen

```text
gradle wrapper        # einmalig, falls gradlew fehlt
./gradlew runIde      # Test-IDE starten
./gradlew verifyPlugin
./gradlew buildPlugin # ZIP in build/distributions/
```

Benötigt JDK 21.

## Struktur

```text
src/main/kotlin/dev/frankbits/classicicons/
├── ClassicIconPatcher.kt       # IconPathPatcher und Cache-Refresh
├── IconRegistry.kt             # zentrale Registry und Manager-Erkennung
├── IconTableModel.kt           # Gruppen und Ausschlusszustand
├── ClassicIconsConfigurable.kt # Settings-UI
├── ClassicIconsSettings.kt     # persistente Einstellungen
└── ClassicIconsStartup.kt      # initiales Registry-Refresh
```
