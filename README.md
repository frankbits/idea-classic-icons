# Classic Icons (New UI)

Das Plugin ersetzt Icons in der New UI durch die klassischen Icons oder extra hinterlegte Custom Icons.

## Einstellungen

Die Einstellungen des Plugins befinden sich unter **Settings | Appearance & Behavior | Classic Icons**.

### Modi

- **Don't use classic icons (New UI)**: Deaktiviert die Classic-Icon-Ersetzung.
- **Use classic icons**: Die ausgewählten Icon-Pfade werden durch die klassischen Icons ersetzt.

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

- Die früheren Modi "**Classic icons for everything**" und "**Classic icons only for files
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
