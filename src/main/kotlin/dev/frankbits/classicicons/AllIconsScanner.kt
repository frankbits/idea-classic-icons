package dev.frankbits.classicicons

import com.intellij.openapi.util.IconLoader
import java.io.File
import javax.swing.Icon

/**
 * Scannt alle verfügbaren Icon-Pfade:
 * - Klassische Icons (Originalpfade)
 * - expui Icons (New UI Icons)
 * 
 * Für jeden expui-Pfad wird geprüft, ob ein klassisches Äquivalent existiert.
 */
object AllIconsScanner {
    
    data class IconInfo(
        val path: String,
        val icon: Icon?,
        val isFileType: Boolean,
        val fileTypes: Set<String>,
        val extensions: Set<String>,
        val hasClassicEquivalent: Boolean,
        val isExpuiIcon: Boolean
    )
    
    private val FILES_AND_FOLDERS_PREFIXES = listOf("/fileTypes/", "/nodes/", "/modules/")
    
    /**
     * Findet alle Icon-Pfade:
     * 1. Klassische Icons (Originalpfade)
     * 2. expui Icons
     * 
     * Für expui Icons wird geprüft, ob ein klassisches Äquivalent existiert.
     */
    fun scanAllIcons(classLoader: ClassLoader): List<IconInfo> {
        val results = mutableListOf<IconInfo>()
        
        // 1. Klassische Icons (Originalpfade) von FileTypes
        FileTypeIcons.refresh()
        for (entry in FileTypeIcons.entries) {
            val path = entry.path
            val hasClassic = classLoader.getResource(path.removePrefix("/")) != null
            
            results.add(IconInfo(
                path = path,
                icon = entry.icon,
                isFileType = true,
                fileTypes = setOf(entry.typeName),
                extensions = if (entry.extension.isNotEmpty()) setOf(entry.extension) else emptySet(),
                hasClassicEquivalent = hasClassic,
                isExpuiIcon = false
            ))
        }
        
        // 2. Weitere klassische Icons (keine FileTypes)
        val classicIconPaths = findClassicIconPaths(classLoader)
        
        for (path in classicIconPaths) {
            if (path.contains("expui/")) continue
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
                extensions = emptySet(),
                hasClassicEquivalent = resourceUrl != null,
                isExpuiIcon = false
            ))
        }
        
        // 3. expui Icons scannen
        val expuiIconPaths = findExpuiIconPaths(classLoader)
        
        for (expuiPath in expuiIconPaths) {
            val originalPath = expuiPath.removePrefix("/expui")
            val hasClassic = classLoader.getResource(originalPath.removePrefix("/")) != null
            val icon = IconLoader.findIcon(expuiPath, classLoader)
            val alreadyAdded = results.any { it.path == originalPath }
            
            if (!alreadyAdded || !hasClassic) {
                results.add(IconInfo(
                    path = originalPath,
                    icon = icon,
                    isFileType = false,
                    fileTypes = emptySet(),
                    extensions = emptySet(),
                    hasClassicEquivalent = hasClassic,
                    isExpuiIcon = true
                ))
            }
        }
        
        return results.sortedBy { it.path }
    }
    
    /**
     * Findet alle klassischen Icon-Pfade im ClassLoader.
     */
    private fun findClassicIconPaths(classLoader: ClassLoader): Set<String> {
        val paths = mutableSetOf<String>()
        
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
        
        for (dir in iconDirectories) {
            val prefix = dir.removePrefix("/")
            try {
                val dirUrl = classLoader.getResource(prefix)
                if (dirUrl != null && dirUrl.protocol == "file") {
                    val dirFile = File(dirUrl.path)
                    if (dirFile.exists() && dirFile.isDirectory) {
                        findIconFilesInDirectory(dirFile, dir).forEach { paths.add(it) }
                    }
                }
            } catch (e: Exception) {
            }
        }
        
        val knownPaths = listOf(
            "/icons/folder.svg",
            "/icons/file.svg",
            "/icons/jar.svg",
            "/icons/class.svg",
            "/icons/method.svg",
            "/icons/field.svg",
            "/icons/parameter.svg",
            "/icons/localVariable.svg",
            "/icons/package.svg",
            "/icons/module.svg",
            "/icons/library.svg",
            "/icons/openFolder.svg",
            "/icons/closedFolder.svg",
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
    
    /**
     * Findet alle expui Icon-Pfade im ClassLoader.
     */
    private fun findExpuiIconPaths(classLoader: ClassLoader): Set<String> {
        val paths = mutableSetOf<String>()
        
        val expuiDirectories = listOf(
            "/expui/icons/",
            "/expui/fileTypes/",
            "/expui/nodes/",
            "/expui/modules/",
            "/expui/actions/",
            "/expui/objects/",
            "/expui/debugger/",
            "/expui/vcs/",
            "/expui/diff/",
            "/expui/editor/",
            "/expui/runConfigurations/",
            "/expui/toolbar/",
            "/expui/toolwindows/",
            "/expui/statusBar/",
            "/expui/navigation/",
        )
        
        for (dir in expuiDirectories) {
            val prefix = dir.removePrefix("/")
            try {
                val dirUrl = classLoader.getResource(prefix)
                if (dirUrl != null && dirUrl.protocol == "file") {
                    val dirFile = File(dirUrl.path)
                    if (dirFile.exists() && dirFile.isDirectory) {
                        findIconFilesInDirectory(dirFile, dir).forEach { paths.add("$dir${it}") }
                    }
                }
            } catch (e: Exception) {
            }
        }
        
        val knownExpuiPaths = listOf(
            "/expui/icons/ai.svg",
            "/expui/icons/copilot.svg",
            "/expui/icons/folder.svg",
            "/expui/icons/file.svg",
            "/expui/icons/jar.svg",
            "/expui/icons/class.svg",
            "/expui/icons/method.svg",
            "/expui/icons/field.svg",
            "/expui/icons/parameter.svg",
            "/expui/icons/localVariable.svg",
            "/expui/icons/package.svg",
            "/expui/icons/module.svg",
            "/expui/icons/library.svg",
            "/expui/fileTypes/rust.svg",
            "/expui/fileTypes/toml.svg",
            "/expui/fileTypes/avro.svg",
            "/expui/fileTypes/proto.svg",
            "/expui/fileTypes/thrift.svg",
            "/expui/fileTypes/graphql.svg",
            "/expui/fileTypes/dart.svg",
            "/expui/fileTypes/go.svg",
            "/expui/fileTypes/swift.svg",
            "/expui/nodes/folder.svg",
            "/expui/nodes/file.svg",
            "/expui/nodes/module.svg",
            "/expui/nodes/package.svg",
            "/expui/nodes/class.svg",
            "/expui/actions/run.svg",
            "/expui/actions/debug.svg",
            "/expui/actions/compile.svg",
            "/expui/actions/save.svg",
            "/expui/actions/saveAll.svg",
            "/expui/actions/sync.svg",
            "/expui/actions/undo.svg",
            "/expui/actions/redo.svg",
            "/expui/objects/gear.svg",
            "/expui/objects/lightbulb.svg",
            "/expui/debugger/db_set_breakpoint.svg",
        )
        
        for (path in knownExpuiPaths) {
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
                result.add("${file.name}")
            }
        }
        
        return result
    }
    
    fun isFileOrFolderIcon(path: String): Boolean {
        return FILES_AND_FOLDERS_PREFIXES.any { path.startsWith(it) } ||
               path.endsWith("File.svg") || 
               path.endsWith("FileType.svg")
    }
    
    fun groupByCategory(icons: List<IconInfo>): Map<String, List<IconInfo>> {
        val categorized = mutableMapOf<String, MutableList<IconInfo>>()
        
        for (icon in icons) {
            val category = getCategoryForPath(icon.path)
            categorized.getOrPut(category) { mutableListOf() }.add(icon)
        }
        
        return categorized
    }
    
    fun getCategoryForPath(path: String): String {
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
