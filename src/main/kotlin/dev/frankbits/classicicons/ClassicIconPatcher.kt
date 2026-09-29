package dev.frankbits.classicicons

import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconPathPatcher

/**
 * Die New UI mappt ORIGINALPFADE (alte Pfade, z. B. "/nodes/folder.svg" oder "/icons/MarkdownPlugin.svg")
 * per Patcher auf die neuen Pfade ("expui/…", bei Plugins z. B. "/icons/expui/markdown.svg" – die Namen
 * unterscheiden sich, es ist nicht nur ein Präfix). Bei normalen Patchern gewinnt der erste, der nicht
 * null liefert. Wir sind vor dem Theme-Patcher installiert und geben für Icons, die es im alten Pfad
 * noch gibt, den Originalpfad zurück – dann greift die New-UI-Umleitung nicht.
 */
object ClassicIconPatcher : IconPathPatcher() {
    /** Anfänge der Originalpfade, die zur "Ordnerstruktur" zählen (nur im Modus "Files and folders"). */
    private val FILES_AND_FOLDERS = listOf("/fileTypes/", "/nodes/", "/modules/")

    override fun patchPath(path: String, classLoader: ClassLoader?): String? {
        if (classLoader == null || path.contains("expui/")) return null

        val state = ClassicIconsSettings.getInstance().state
        if (state.scope == IconScope.FILES_AND_FOLDERS && !isFileOrFolderIcon(path, state.extraFilters)) return null

        // Nur eingreifen, wenn das klassische Icon wirklich existiert
        return if (classLoader.getResource(path.removePrefix("/")) != null) path else null
    }

    private fun isFileOrFolderIcon(path: String, extraFilters: String): Boolean =
        FILES_AND_FOLDERS.any { path.startsWith(it) } ||
            path.endsWith("File.svg") || path.endsWith("FileType.svg") ||
            extraFilters.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.any { path.contains(it) }

    override fun getContextClassLoader(path: String, originalClassLoader: ClassLoader?): ClassLoader? =
        originalClassLoader

    fun install() = IconLoader.installPathPatcher(this)
}

/** Application component: wird früh beim Start instanziiert (wie bei IdeaIconPack). */
class ClassicIconsComponent {
    init {
        ClassicIconPatcher.install()
    }
}
