package dev.frankbits.classicicons

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconPathPatcher
import java.io.File
import javax.swing.Icon

/**
 * Restores classic icon paths before the New UI theme patcher can replace them.
 *
 * Custom files take precedence. Otherwise the original path is returned only
 * when the resource exists and the current scope/exclusion settings allow it.
 */
object ClassicIconPatcher : IconPathPatcher() {
    private val loadingPreview = ThreadLocal.withInitial { false }

    /** Loads the original icon without allowing this patcher to replace it. */
    fun loadOriginalIcon(path: String, classLoader: ClassLoader): Icon? {
        if (classLoader.getResource(path.removePrefix("/")) == null) return null
        loadingPreview.set(true)
        return try {
            IconLoader.findIcon(path.removePrefix("/"), classLoader)
        } finally {
            loadingPreview.set(false)
        }
    }

    /** Path prefixes treated as file/folder icons in the restricted scope. */
    private val FILES_AND_FOLDERS = listOf("/fileTypes/", "/nodes/", "/modules/")

    /**
     * Records a requested path and returns the classic path when replacement is allowed.
     *
     * Returning null delegates resolution to IntelliJ's remaining patchers.
     */
    override fun patchPath(path: String, classLoader: ClassLoader?): String? {
        if (loadingPreview.get()) return null
        val normalizedPath = path.ensureLeadingSlash()
        if (normalizedPath.contains("expui/")) return null
        val state = ClassicIconsSettings.getInstance().state

        customIcon(normalizedPath, state.customIconsDir)?.let { return it }
        if (normalizedPath in state.excludedIconPaths) return null
        if (classLoader == null) return null

        if (classLoader.getResource(path.removePrefix("/")) == null) return null
        IconRegistry.recordPath(
            normalizedPath,
            loadOriginalIcon(normalizedPath, classLoader),
            classLoader
        )
        val cachedPaths = state.cachedRuntimeIconPaths
        if (normalizedPath !in cachedPaths) {
            cachedPaths.add(normalizedPath)
        }
        if (state.scope == IconScope.DISABLED) return null

        // Nur eingreifen, wenn das klassische Icon wirklich existiert
        return path
    }

    private fun String.ensureLeadingSlash(): String =
        if (startsWith("/")) this else "/$this"

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
    internal fun isFileOrFolderIcon(
        path: String,
        state: ClassicIconsSettings.State,
    ): Boolean {
        // Check path patterns
        if (FILES_AND_FOLDERS.any { path.startsWith(it) }) return true

        // Registered FileType icons are part of the files-and-folders preset.
        IconRegistry.typesFor(path)?.let { types ->
            if (types.isNotEmpty()) return true
        }

        // Check extra filters
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
