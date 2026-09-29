package dev.frankbits.classicicons

import com.intellij.openapi.components.*

enum class IconScope { DISABLED, ALL, FILES_AND_FOLDERS }

@Service(Service.Level.APP)
@State(name = "ClassicIconsSettings", storages = [Storage("classicIcons.xml")])
class ClassicIconsSettings : PersistentStateComponent<ClassicIconsSettings.State> {
    class State {
        var scope: IconScope = IconScope.ALL

        /** Zus\u00e4tzliche Pfad-Teile (ein Eintrag pro Zeile), die im Modus "Files and folders" als Datei-Icons z\u00e4hlen. */
        var extraFilters: String = ""

        /** Ordner mit eigenen Icons; Struktur spiegelt die Original-Icon-Pfade (z. B. fileTypes/java.svg). */
        var customIconsDir: String = ""

        /** Dateitypen (Name), deren Icon im Modus "Files and folders" NICHT klassisch sein soll. */
        var excludedFileTypes: MutableList<String> = mutableListOf()

        /** Benutzerdefinierte Pfadfilter pro Dateityp (Name -> Filter). */
        var customPathFilters: MutableMap<String, String> = mutableMapOf()
    }

    private var state = State()
    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    companion object {
        fun getInstance(): ClassicIconsSettings = service()
    }
}
