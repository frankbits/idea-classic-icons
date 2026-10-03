# Classic Icons (New UI)

JetBrains-Plugin für IntelliJ Platform ab Build 243. Das Plugin verwendet in der New UI
klassische Icon-Pfade, ohne die gesamte New UI wie ClassicUI abzuschalten.

Einstellungen befinden sich unter **Settings | Appearance & Behavior | Classic Icons**.
Die Icon-Liste wird aus zwei Quellen aufgebaut:

- **Registered icons**: FileTypeManager und ActionManager liefern bekannte Icons inklusive
  FileType-/Action-Metadaten.
- **Runtime-discovered icons**: weitere Pfade werden registriert, wenn IntelliJ sie
  tatsächlich anfordert. Die gefundenen Pfade werden für spätere IDE-Sitzungen gespeichert;
  die Liste kann trotzdem unvollständig sein und zwischendurch veraltete Einträge enthalten.
  Über **Validate cached runtime paths** können gespeicherte Runtime-Pfade gegen die in der
  Sitzung bekannten ClassLoader geprüft werden. Nicht auflösbare Pfade können im Dialog
  einzeln zur Entfernung ausgewählt werden.

Die Gruppen sind nach dem ersten Pfadsegment gegliedert und können unabhängig voneinander
auf- und zugeklappt werden. Gruppen-Checkboxen haben drei Zustände: vollständig aktiv,
vollständig deaktiviert und teilweise aktiv. Einzelne Icons können ebenfalls ausgeschlossen
werden. Eine Gruppe mit gleichem Namen in den beiden Quellen wird getrennt behandelt.

## Modi

- **Don't use classic icons (New UI)**: Der Patcher bleibt deaktiviert.
- **Classic icons for everything**: Alle bekannten klassischen Pfade werden berücksichtigt.
- **Use classic icons**: Aktiviert die individuell ausgewählten Icons.
- **Presets in der Icon-Liste**: **All icons** aktiviert alle bekannten Icons;
  **Files and folders** aktiviert die erkannten Datei-/Ordnerpfade. Beide Presets
  ändern nur die Checkboxen im aktuellen Einstellungsdialog und werden erst mit
  **Apply** gespeichert. Einzelne Icons können danach weiterhin angepasst werden.

Ein Custom-Icon-Verzeichnis kann die Originalpfade spiegeln, z. B.
`fileTypes/java.svg` oder `icons/MarkdownPlugin.svg`. SVG- und PNG-Dateien werden unterstützt
und überschreiben die klassische Vorschau.

## Migrations-Info:

### Upgrade to `1.0.0`:
Die Einstellungen der Pre-Release-Version `0.1.0` werden wegen der grundlegenden
Änderungen an der Auswahl nicht vollständig übernommen:

- Die Modi "**Classic icons for everything**" und "**Classic icons only for files
  and folders**" wurden zu den Presets "**All icons**" und "**Files and folders**".  
  Bitte das jeweilige Preset wählen und Änderungen speichern, um das Verhalten der Modi wiederherzustellen.
- Icons mit Dateinamen, die auf `File.svg` oder `FileType.svg` enden, werden nicht mehr automatisch zu "**Files and folders**" gezählt.  
  Falls diese Icons in der neuen Version verwendet werden sollen, muss die Auswahl der FileType-Icons gegebenenfalls angepasst werden.
- Einschränkung der zu ersetzenden FileType-Icons werden nicht migriert.
  Die Icon-Auswahl muss gegebenenfalls neu gesetzt werden.


- "**Advanced: additional path filters**" wird weiterhin vom Preset "**Files and folders**" berücksichtigt.
- "**Custom icon pack**" bleibt erhalten.
- Wenn **Classic Icons** deaktiviert waren, bleiben sie weiterhin deaktiviert.

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
