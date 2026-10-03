package dev.frankbits.classicicons

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconPathPatcher
import java.io.File

/**
 * Restores classic icon paths before the New UI theme patcher can replace them.
 *
 * Custom files take precedence. Otherwise the original path is returned only
 * when the resource exists and the current scope/exclusion settings allow it.
 */
object ClassicIconPatcher : IconPathPatcher() {
    private val loadingPreview = ThreadLocal.withInitial { false }

    /** Path prefixes treated as file/folder icons in the restricted scope. */
    private val FILES_AND_FOLDERS = listOf("/fileTypes/", "/nodes/", "/modules/")

    /**
     * Records a requested path and returns the classic path when replacement is allowed.
     *
     * Returning null delegates resolution to IntelliJ's remaining patchers.
     */
    override fun patchPath(path: String, classLoader: ClassLoader?): String? {
        if (loadingPreview.get()) return null
        if (path.contains("expui/")) return null
        val state = ClassicIconsSettings.getInstance().state

        customIcon(path, state.customIconsDir)?.let { return it }
        if (path in state.excludedIconPaths) return null
        if (classLoader == null) return null

        if (classLoader.getResource(path.removePrefix("/")) == null) return null
        val resource = classLoader.getResource(path.removePrefix("/")) ?: return null
        loadingPreview.set(true)
        try {
            IconLoader.findIcon(resource)?.let { IconRegistry.recordPath(path, it) }
                ?: IconRegistry.recordPath(path)
        } finally {
            loadingPreview.set(false)
        }
        IconRegistry.typesFor(path)?.let { types ->
            if (types.isNotEmpty() && types.all { it in state.excludedFileTypes }) return null
        }

        when (state.scope) {
            IconScope.DISABLED -> return null
            IconScope.FILES_AND_FOLDERS -> if (!isFileOrFolderIcon(path, state)) return null
            IconScope.ALL -> Unit
        }

        // Nur eingreifen, wenn das klassische Icon wirklich existiert
        return path
    }

    /** Resolves a custom SVG or PNG override for an original icon path. */
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

    /** Checks whether [path] belongs to the restricted files-and-folders scope. */
    private fun isFileOrFolderIcon(path: String, state: ClassicIconsSettings.State): Boolean {
        // First: if ALL file types using this icon path are excluded, don't use classic icon
        IconRegistry.typesFor(path)?.let { types ->
            if (types.all { it in state.excludedFileTypes }) return false
        }

        // Then check path patterns
        if (FILES_AND_FOLDERS.any { path.startsWith(it) }) return true
        if (path.endsWith("File.svg") || path.endsWith("FileType.svg")) return true

        // Check if any non-excluded file type uses this path
        IconRegistry.typesFor(path)?.let { types ->
            if (types.any { it !in state.excludedFileTypes }) return true
        }

        return state.extraFilters.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.any { path.contains(it) }
    }

    /** Preserves the loader used by IntelliJ for the original icon resource. */
    override fun getContextClassLoader(path: String, originalClassLoader: ClassLoader?): ClassLoader? =
        originalClassLoader

    /** Registers this patcher with IntelliJ's global icon loader. */
    fun install() = IconLoader.installPathPatcher(this)

    /** Clears the icon cache and refreshes open project views. */
    fun refreshUi() {
        IconLoader.clearCache()
        ApplicationManager.getApplication().invokeLater {
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) ProjectView.getInstance(project).refresh()
            }
        }
    }
}

/** Installs the patcher early during application startup. */
class ClassicIconsComponent {
    /** Installs [ClassicIconPatcher] when the application component is created. */
    init {
        ClassicIconPatcher.install()
    }
}
