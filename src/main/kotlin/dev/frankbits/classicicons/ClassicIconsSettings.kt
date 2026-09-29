package dev.frankbits.classicicons

import com.intellij.openapi.components.*

enum class IconScope { ALL, FILES_AND_FOLDERS }

@Service(Service.Level.APP)
@State(name = "ClassicIconsSettings", storages = [Storage("classicIcons.xml")])
class ClassicIconsSettings : PersistentStateComponent<ClassicIconsSettings.State> {
    class State {
        var scope: IconScope = IconScope.ALL
        /** Zusätzliche Pfad-Teile (ein Eintrag pro Zeile), die im Modus "Files and folders" als Datei-Icons zählen. */
        var extraFilters: String = "MarkdownPlugin"
    }

    private var state = State()
    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    companion object {
        fun getInstance(): ClassicIconsSettings = service()
    }
}
