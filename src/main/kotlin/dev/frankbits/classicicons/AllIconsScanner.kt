package dev.frankbits.classicicons

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.icons.IconPathProvider
import java.io.File
import java.net.URL
import javax.swing.Icon

/**
 * Scannt alle verfügbaren Icon-Pfade, die durch den ClassicIconPatcher ersetzt werden können.
 * Dies umfasst:
 * - FileType Icons
 * - Alle anderen Icons aus dem ClassLoader
 * - Icons aus dem expui/-Verzeichnis (werden nicht ersetzt, aber zur Referenz)
 */
object AllIconsScanner {
    
    data class IconInfo(
        val path: String,
        val icon: Icon?,
        val isFileType: Boolean,
        val fileTypes: Set<String>,
        val extensions: Set<String>
    )
    
    private val FILES_AND_FOLDERS_PREFIXES = listOf("/fileTypes/", "/nodes/", "/modules/")
    
    /**
     * Findet alle Icon-Pfade, die potenziell ersetzt werden können.
     * Dies sind alle Icons, die:
     * 1. Nicht im expui/-Verzeichnis liegen
     * 2. Im ClassLoader verfügbar sind
     */
    fun scanAllAvailableIcons(classLoader: ClassLoader): List<IconInfo> {
        val results = mutableListOf<IconInfo>()
        
        // 1. FileType Icons hinzufügen
        FileTypeIcons.refresh()
        for (entry in FileTypeIcons.entries) {
            results.add(IconInfo(
                path = entry.path,
                icon = entry.icon,
                isFileType = true,
                fileTypes = setOf(entry.typeName),
                extensions = if (entry.extension.isNotEmpty()) setOf(entry.extension) else emptySet()
            ))
        }
        
        // 2. Weitere Icons aus dem ClassLoader finden
        // Wir versuchen, alle SVG/PNG Dateien im ClassLoader zu finden
        val iconPaths = findIconPathsInClassLoader(classLoader)
        
        for (path in iconPaths) {
            // Skip expui icons
            if (path.contains("expui/")) continue
            
            // Skip if already added as file type
            if (results.any { it.path == path }) continue
            
            val resourceUrl = classLoader.getResource(path.removePrefix("/"))
            val icon = if (resourceUrl != null) {
                IconLoader.findIcon(path, classLoader)
            } else {
                null
            }
            
            results.add(IconInfo(
                path = path,
                icon = icon,
                isFileType = false,
                fileTypes = emptySet(),
                extensions = emptySet()
            ))
        }
        
        return results.sortedBy { it.path }
    }
    
    /**
     * Versucht, alle Icon-Pfade im ClassLoader zu finden.
     * Dies ist eine Heuristik, da es keine direkte API gibt, um alle Icons aufzulisten.
     */
    private fun findIconPathsInClassLoader(classLoader: ClassLoader): Set<String> {
        val paths = mutableSetOf<String>()
        
        // Bekannte Icon-Verzeichnisse
        val iconDirectories = listOf(
            "/icons/",
            "/fileTypes/",
            "/nodes/",
            "/modules/",
            "/actions/",
            "/objects/",
            "/debugger/",
            "/vcs/",
            "/diff/",
            "/editor/",
            "/runConfigurations/",
            "/toolbar/",
            "/toolwindows/",
            "/statusBar/",
            "/navigation/",
            "/hierarchy/",
            "/scope/",
            "/log/",
        )
        
        // Für jedes Verzeichnis, versuche Dateien zu finden
        for (dir in iconDirectories) {
            val prefix = dir.removePrefix("/")
            try {
                // Versuche, das Verzeichnis als Ressource zu lesen
                val dirUrl = classLoader.getResource(prefix)
                if (dirUrl != null && dirUrl.protocol == "file") {
                    val dirFile = File(dirUrl.path)
                    if (dirFile.exists() && dirFile.isDirectory) {
                        findIconFilesInDirectory(dirFile, "$dir").forEach { paths.add(it) }
                    }
                }
            } catch (e: Exception) {
                // Ignorieren - nicht alle ClassLoader unterstützen Dateisystemzugriff
            }
        }
        
        // zusätzlich: versuche bekannte Icon-Pfade
        val knownPaths = listOf(
            "/icons/MarkdownPlugin.svg",
            "/icons/AllIcons.svg",
            "/icons/PluginIcons.svg",
            "/icons/jar.svg",
            "/icons/class.svg",
            "/icons/method.svg",
            "/icons/field.svg",
            "/icons/parameter.svg",
            "/icons/localVariable.svg",
            "/icons/package.svg",
            "/icons/module.svg",
            "/icons/library.svg",
            "/icons/folder.svg",
            "/icons/openFolder.svg",
            "/icons/closedFolder.svg",
            "/icons/file.svg",
            "/icons/textFile.svg",
            "/icons/javaFile.svg",
            "/icons/kotlinFile.svg",
            "/icons/xmlFile.svg",
            "/icons/jsonFile.svg",
            "/icons/htmlFile.svg",
            "/icons/cssFile.svg",
            "/icons/jsFile.svg",
            "/icons/tsFile.svg",
            "/icons/pythonFile.svg",
            "/icons/sqlFile.svg",
            "/icons/propertiesFile.svg",
            "/icons/ymlFile.svg",
            "/icons/markdownFile.svg",
            "/nodes/folder.svg",
            "/nodes/file.svg",
            "/nodes/module.svg",
            "/nodes/package.svg",
            "/nodes/class.svg",
            "/actions/run.svg",
            "/actions/debug.svg",
            "/actions/compile.svg",
            "/actions/save.svg",
            "/actions/saveAll.svg",
            "/actions/sync.svg",
            "/actions/undo.svg",
            "/actions/redo.svg",
            "/actions/copy.svg",
            "/actions/cut.svg",
            "/actions/paste.svg",
            "/actions/delete.svg",
            "/actions/new.svg",
            "/actions/open.svg",
            "/actions/close.svg",
            "/actions/find.svg",
            "/actions/replace.svg",
            "/actions/refactor.svg",
            "/actions/runClass.svg",
            "/actions/runTest.svg",
            "/actions/debugClass.svg",
            "/objects/gear.svg",
            "/objects/lightbulb.svg",
            "/objects/information.svg",
            "/objects/warning.svg",
            "/objects/error.svg",
            "/debugger/db_set_breakpoint.svg",
            "/debugger/db_verify_breakpoint.svg",
            "/debugger/db_disabled_breakpoint.svg",
            "/vcs/changes.svg",
            "/vcs/commit.svg",
            "/vcs/update.svg",
            "/vcs/push.svg",
            "/vcs/pull.svg",
        )
        
        for (path in knownPaths) {
            if (classLoader.getResource(path.removePrefix("/")) != null) {
                paths.add(path)
            }
        }
        
        return paths
    }
    
    private fun findIconFilesInDirectory(dir: File, basePath: String): List<String> {
        val result = mutableListOf<String>()
        
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                result.addAll(findIconFilesInDirectory(file, "$basePath${file.name}/"))
            } else if (file.isFile && (file.name.endsWith(".svg") || file.name.endsWith(".png"))) {
                result.add("$basePath${file.name}")
            }
        }
        
        return result
    }
    
    /**
     * Prüft, ob ein Icon-Pfad zu den FileTypes/Folders gehört
     */
    fun isFileOrFolderIcon(path: String): Boolean {
        return FILES_AND_FOLDERS_PREFIXES.any { path.startsWith(it) } ||
               path.endsWith("File.svg") || 
               path.endsWith("FileType.svg")
    }
    
    /**
     * Gruppiert Icons nach Kategorien
     */
    fun groupByCategory(icons: List<IconInfo>): Map<String, List<IconInfo>> {
        val categorized = mutableMapOf<String, MutableList<IconInfo>>()
        
        for (icon in icons) {
            val category = getCategoryForPath(icon.path)
            categorized.getOrPut(category) { mutableListOf() }.add(icon)
        }
        
        return categorized
    }
    
    private fun getCategoryForPath(path: String): String {
        return when {
            path.startsWith("/fileTypes/") -> "File Types"
            path.startsWith("/nodes/") -> "Nodes"
            path.startsWith("/modules/") -> "Modules"
            path.startsWith("/icons/") -> "General Icons"
            path.startsWith("/actions/") -> "Actions"
            path.startsWith("/objects/") -> "Objects"
            path.startsWith("/debugger/") -> "Debugger"
            path.startsWith("/vcs/") -> "Version Control"
            path.startsWith("/diff/") -> "Diff"
            path.startsWith("/editor/") -> "Editor"
            path.startsWith("/runConfigurations/") -> "Run Configurations"
            path.startsWith("/toolbar/") -> "Toolbar"
            path.startsWith("/toolwindows/") -> "Tool Windows"
            path.startsWith("/statusBar/") -> "Status Bar"
            else -> "Other"
        }
    }
}
