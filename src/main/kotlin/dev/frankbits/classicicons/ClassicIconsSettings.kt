package dev.frankbits.classicicons

import com.intellij.openapi.components.*

enum class IconScope { DISABLED, ALL, FILES_AND_FOLDERS }

@Service(Service.Level.APP)
@State(name = "ClassicIconsSettings", storages = [Storage("classicIcons.xml")])
class ClassicIconsSettings : PersistentStateComponent<ClassicIconsSettings.State> {
    class State {
        var scope: IconScope = IconScope.ALL

        /** Zusätzliche Pfad-Teile (ein Eintrag pro Zeile), die im Modus "Files and folders" als Datei-Icons zählen. */
        var extraFilters: String = ""

        /** Ordner mit eigenen Icons; Struktur spiegelt die Original-Icon-Pfade (z. B. fileTypes/java.svg). */
        var customIconsDir: String = ""

        /** Dateitypen (Name), deren Icon im Modus "Files and folders" NICHT klassisch sein soll. */
        var excludedFileTypes: MutableList<String> = mutableListOf()

        /** Icon-Mappings: mappt Icon-Pfade auf andere Pfade (z. B. "/actions/rerun.svg" -> "/actions/restart.svg"). */
        var iconMappings: MutableMap<String, String> = mutableMapOf()
    }

    private var state = State()
    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    companion object {
        fun getInstance(): ClassicIconsSettings = service()
    }
}
