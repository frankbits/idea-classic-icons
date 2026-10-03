package dev.frankbits.classicicons

import com.intellij.openapi.components.*

/** Defines the set of icon paths eligible for classic icon replacement. */
enum class IconScope { DISABLED, ENABLED }

/** Persistent application settings for classic icon replacement. */
@Service(Service.Level.APP)
@State(name = "ClassicIconsSettings", storages = [Storage("classicIcons.xml")])
class ClassicIconsSettings : PersistentStateComponent<ClassicIconsSettings.State> {
    /** Serializable settings state stored in `classicIcons.xml`. */
    class State {
        /** Determines which icon paths the patcher may replace. */
        var scope: IconScope = IconScope.ENABLED

        /** Path fragments treated as file/folder icons in the restricted scope. */
        var extraFilters: String = ""

        /** Directory whose relative structure mirrors original icon paths. */
        var customIconsDir: String = ""

        /** Icon paths that are explicitly excluded from replacement. */
        var excludedIconPaths: MutableList<String> = mutableListOf()

        /** Runtime-discovered paths cached for display across IDE sessions. */
        var cachedRuntimeIconPaths: MutableList<String> = mutableListOf()
    }

    private var state = State()
    /** Returns the current persisted settings. */
    override fun getState(): State = state

    /** Replaces the in-memory settings with the persisted state. */
    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        /** Returns the application-level settings service. */
        fun getInstance(): ClassicIconsSettings = service()
    }
}
