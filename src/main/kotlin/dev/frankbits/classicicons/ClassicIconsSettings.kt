package dev.frankbits.classicicons

import com.intellij.openapi.components.*

/** Defines whether the path-based icon selection is active. */
enum class IconScope {
    /** Keep all New UI icons, also ignoring custom icon overrides. */
    DISABLED,

    /** Use only custom icon overrides and never replace icons with classic paths. */
    CUSTOM_ONLY,

    /** Apply classic replacements while ignoring custom icon overrides. */
    CLASSIC_ONLY,

    /** Apply classic replacements for selected icon paths. */
    ENABLED
}

/** Persistent application settings for classic icon replacement. */
@Service(Service.Level.APP)
@State(name = "ClassicIconsSettings", storages = [Storage("classicIcons.xml")])
class ClassicIconsSettings : PersistentStateComponent<ClassicIconsSettings.State> {
    /** Serializable settings state stored in `classicIcons.xml`. */
    class State {
        /** Determines whether selected icon paths may be replaced. */
        var scope: IconScope = IconScope.ENABLED

        /** Path fragments included by the Files and folders preset. */
        var extraFilters: String = ""

        /** Directory whose relative structure mirrors original icon paths. */
        var customIconsDir: String = ""

        /** Icon paths explicitly excluded from classic replacement. */
        var excludedIconPaths: MutableList<String> = mutableListOf()

        /** Runtime-discovered icon paths cached across IDE sessions. */
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
