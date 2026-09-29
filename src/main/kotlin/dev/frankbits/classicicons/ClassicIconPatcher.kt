package dev.frankbits.classicicons

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconPathPatcher
import java.io.File

/**
 * Die New UI mappt ORIGINALPFADE (alte Pfade, z. B. "/nodes/folder.svg" oder "/icons/MarkdownPlugin.svg")
 * per Patcher auf die neuen Pfade. Bei normalen Patchern gewinnt der erste, der nicht null liefert.
 * Wir sind vor dem Theme-Patcher installiert und geben
 *  1. für Icons aus dem eigenen Icon-Pack-Ordner eine file:-URL zurück,
 *  2. für Icons, die es im alten Pfad noch gibt, den Originalpfad zurück.
 */
object ClassicIconPatcher : IconPathPatcher() {
    /** Anfänge der Originalpfade, die zur "Ordnerstruktur" zählen (nur im Modus "Files and folders"). */
    private val FILES_AND_FOLDERS = listOf("/fileTypes/", "/nodes/", "/modules/")

    override fun patchPath(path: String, classLoader: ClassLoader?): String? {
        if (path.contains("expui/")) return null
        val state = ClassicIconsSettings.getInstance().state

        customIcon(path, state.customIconsDir)?.let { return it }

        if (classLoader == null) return null
        when (state.scope) {
            IconScope.DISABLED -> return null
            IconScope.FILES_AND_FOLDERS -> if (!isFileOrFolderIcon(path, state)) return null
            IconScope.ALL -> Unit
        }

        // Nur eingreifen, wenn das klassische Icon wirklich existiert
        return if (classLoader.getResource(path.removePrefix("/")) != null) path else null
    }

    private fun customIcon(path: String, dir: String): String? {
        if (dir.isBlank()) return null
        val rel = path.removePrefix("/")
        val exact = File(dir, rel)
        if (exact.isFile) return exact.toURI().toString()
        val base = rel.substringBeforeLast('.', rel)
        for (ext in listOf("svg", "png")) {
            val f = File(dir, "$base.$ext")
            if (f.isFile) return f.toURI().toString()
        }
        return null
    }

    private fun isFileOrFolderIcon(path: String, state: ClassicIconsSettings.State): Boolean {
        if (FILES_AND_FOLDERS.any { path.startsWith(it) }) return true
        if (path.endsWith("File.svg") || path.endsWith("FileType.svg")) return true
        FileTypeIcons.typesFor(path)?.let { types ->
            if (types.any { it !in state.excludedFileTypes }) return true
        }
        return state.extraFilters.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.any { path.contains(it) }
    }

    override fun getContextClassLoader(path: String, originalClassLoader: ClassLoader?): ClassLoader? =
        originalClassLoader

    fun install() = IconLoader.installPathPatcher(this)

    /** Icon-Cache leeren und den Projektbaum neu zeichnen. */
    fun refreshUi() {
        IconLoader.clearCache()
        ApplicationManager.getApplication().invokeLater {
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) ProjectView.getInstance(project).refresh()
            }
        }
    }
}

/** Application component: wird früh beim Start instanziiert (wie bei IdeaIconPack). */
class ClassicIconsComponent {
    init {
        ClassicIconPatcher.install()
    }
}
