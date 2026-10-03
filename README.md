# Classic Icons (New UI)

Das Plugin ersetzt Icons in der New UI durch die klassischen Icons oder extra hinterlegte Custom Icons.
Unterstützt werden IntelliJ-Plattform-Versionen ab Build 242 (IntelliJ IDEA 2024.2), seit dem die New UI standardmäßig aktiviert ist.

## Einstellungen

Die Einstellungen des Plugins befinden sich unter **Settings | Appearance & Behavior | Classic Icons**.

### Modi

- **Disable icon replacement**: Deaktiviert jede Ersetzung, einschließlich
  Custom-Icons.
- **Custom icons only**: Verwendet ausschließlich vorhandene Custom-Icon-Overrides.
- **Classic icons only**: Verwendet nur die ausgewählten klassischen Icon-Pfade und
  ignoriert Custom-Icon-Overrides.
- **Custom & Classic icons**: Verwendet Custom-Icons, sofern vorhanden, und ersetzt
  ansonsten die ausgewählten Pfade durch klassische Icons.

### Presets

- **All icons**: Aktiviert alle aktuell registrierten Icon-Pfade.
- **Files and folders**: Aktiviert erkannte Datei-/Ordnerpfade sowie registrierte
  FileType-Icons und zusätzliche Pfadfilter ("**Advanced: additional path filters**").

Presets arbeiten auf dem ungespeicherten Einstellungszustand, durch Vorbelegung der Icon-Auswahl.
Sie werden erst mit **Apply** gespeichert und können somit weiter angepasst werden.

### Icon-Auswahl

Die Icon-Liste wird aus zwei Quellen aufgebaut:

- **Registered icons**: FileTypeManager und ActionManager liefern bekannte Icons inklusive
  FileType-/Action-Metadaten.
- **Runtime-discovered icons**: weitere Pfade werden registriert, wenn IntelliJ sie
  tatsächlich anfordert. Die gefundenen Pfade werden für spätere IDE-Sitzungen gespeichert;
  die Liste kann trotzdem unvollständig sein und zwischendurch veraltete Einträge enthalten.
  Über **Validate cached runtime paths** können gespeicherte Runtime-Pfade gegen die in der
  Sitzung bekannten ClassLoader geprüft werden. Nicht auflösbare Pfade können im Dialog
  einzeln zur Entfernung ausgewählt werden.

Einzelne Icon-Pfade können von der Ersetzung der neuen mit den klassischen Icons ausgeschlossen werden.
Die Gruppen sind nach dem ersten Pfadsegment gegliedert und können unabhängig voneinander
auf- und zugeklappt werden.
Alle Icons einer Gruppe können auf einmal ausgewählt oder abgewählt werden.
Registrierte und Runtime-Icons werden getrennt dargestellt und behandelt.

### Custom Icon-Pack

Ein Custom-Icon-Verzeichnis kann die Originalpfade spiegeln, z. B.
`fileTypes/java.svg` oder `icons/MarkdownPlugin.svg`. SVG- und PNG-Dateien werden unterstützt
und haben Vorrang vor der Ersetzung mit klassischen Icons.

## Migrations-Info:

### Upgrade to `0.2.0`:

Die Einstellungen der Pre-Release-Version `0.1.0` werden wegen der geänderten
Auswahllogik nicht vollständig übernommen:

- **Classic icons for everything** wird durch den Modus **Custom & Classic icons**
  mit dem Preset **All icons** ersetzt.
- **Classic icons only for files and folders** wird durch den Modus
  **Custom & Classic icons** mit dem Preset **Files and folders** ersetzt.
- **Don't use classic icons (New UI)** wird durch den Modus **Custom icons only** ersetzt.
- **Don't use classic icons (New UI)** entspricht dem Modus **Disable icon replacement**.
  Wenn also Custom Icons ersetzt werden sollen, muss zum Modus **Custom icons only** gewechselt werden.
- Einschränkung der zu ersetzenden FileType-Icons werden nicht migriert.
  Die Icon-Auswahl muss gegebenenfalls neu gesetzt werden.


- "**Advanced: additional path filters**" wird weiterhin vom Preset "**Files and folders**" berücksichtigt.
- "**Custom icon pack**" bleibt erhalten.

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
