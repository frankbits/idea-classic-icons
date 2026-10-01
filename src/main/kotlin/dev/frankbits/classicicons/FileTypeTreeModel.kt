package dev.frankbits.classicicons

import java.io.File
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Baumstruktur für alle Icons, die durch den ClassicIconPatcher ersetzt werden können.
 */
class FileTypeTreeModel : DefaultTreeModel(DefaultMutableTreeNode("Icons")) {
    private var customIconsDir: String = ""
    private val root get() = super.getRoot() as DefaultMutableTreeNode
    
    private var allIcons: List<AllIconsScanner.IconInfo> = emptyList()

    class IconPathNode(
        val path: String,
        val icon: Icon?,
        val isFileType: Boolean,
        val fileTypes: Set<String>,
        val extensions: Set<String>,
        val hasClassicEquivalent: Boolean,
        val isExpuiIcon: Boolean,
        var classic: Boolean
    ) : DefaultMutableTreeNode(path) {
        override fun toString(): String = path
    }

    class FileTypeNode(
        val fileType: String,
        val path: String,
        val extension: String,
        var classic: Boolean
    ) : DefaultMutableTreeNode(fileType) {
        override fun toString(): String = fileType
    }

    class CategoryNode(val category: String) : DefaultMutableTreeNode(category) {
        override fun toString(): String = category
    }

    fun load(excluded: Collection<String>, classLoader: ClassLoader, scope: IconScope = IconScope.ALL) {
        val ex = excluded.toSet()
        root.removeAllChildren()

        val allIconsList = AllIconsScanner.scanAllIcons(classLoader)
        allIcons = allIconsList

        val groupedByCategory = allIconsList.groupBy { 
            AllIconsScanner.getCategoryForPath(it.path) 
        }

        val allIconsRoot = CategoryNode("All Icons")

        for ((category, icons) in groupedByCategory.entries.sortedBy { it.key }) {
            val categoryNode = CategoryNode(category)
            
            val groupedByPath = icons.groupBy { it.path }
            
            for ((path, pathIcons) in groupedByPath.entries.sortedBy { it.key }) {
                val firstIcon = pathIcons.first()
                val allExcluded = pathIcons.all { entry -> 
                    entry.fileTypes.all { ft -> ft in ex } 
                }
                
                val shouldBeClassic = shouldShowClassic(path, scope, firstIcon.hasClassicEquivalent)
                
                val pathNode = IconPathNode(
                    path = path,
                    icon = firstIcon.icon,
                    isFileType = firstIcon.isFileType,
                    fileTypes = pathIcons.flatMap { it.fileTypes }.toSet(),
                    extensions = pathIcons.flatMap { it.extensions }.toSet(),
                    hasClassicEquivalent = firstIcon.hasClassicEquivalent,
                    isExpuiIcon = firstIcon.isExpuiIcon,
                    classic = !allExcluded && shouldBeClassic
                )
                
                if (firstIcon.isFileType) {
                    val entries = icons.filter { it.path == path }
                    for (entry in entries.sortedBy { it.fileTypes.firstOrNull() ?: "" }) {
                        for (fileType in entry.fileTypes) {
                            val isExcluded = fileType in ex
                            val fileTypeNode = FileTypeNode(
                                fileType = fileType, 
                                path = path, 
                                extension = entry.extensions.firstOrNull() ?: "", 
                                classic = !isExcluded
                            )
                            pathNode.add(fileTypeNode)
                        }
                    }
                }
                
                categoryNode.add(pathNode)
            }
            
            if (categoryNode.childCount > 0) {
                allIconsRoot.add(categoryNode)
            }
        }
        
        if (allIconsRoot.childCount > 0) {
            root.add(allIconsRoot)
        }

        reload()
    }

    private fun shouldShowClassic(path: String, scope: IconScope, hasClassicEquivalent: Boolean): Boolean {
        if (!hasClassicEquivalent) {
            return true
        }
        
        return when (scope) {
            IconScope.DISABLED -> false
            IconScope.ALL -> true
            IconScope.FILES_AND_FOLDERS -> {
                val filesAndFolders = listOf("/fileTypes/", "/nodes/", "/modules/")
                filesAndFolders.any { path.startsWith(it) } ||
                path.endsWith("File.svg") || 
                path.endsWith("FileType.svg")
            }
        }
    }

    fun setCustomIconsDir(dir: String) {
        this.customIconsDir = dir
        reload()
    }

    fun hasCustomIcon(node: DefaultMutableTreeNode): Boolean {
        if (customIconsDir.isBlank()) return false

        val pathNode = node as? IconPathNode ?: return false
        val rel = pathNode.path.removePrefix("/")
        val exact = File(customIconsDir, rel)
        if (exact.isFile) return true

        val base = rel.substringBeforeLast('.', rel)
        for (ext in listOf("svg", "png")) {
            val f = File(customIconsDir, "$base.$ext")
            if (f.isFile) return true
        }
        return false
    }

    fun getIconTooltip(node: DefaultMutableTreeNode): String? {
        when (val userObject = node.userObject) {
            is IconPathNode -> {
                if (hasCustomIcon(node)) {
                    return "Overridden by custom icon"
                }
                if (!userObject.hasClassicEquivalent) {
                    return "No classic equivalent - can only be replaced by custom icon"
                }
                if (userObject.isExpuiIcon) {
                    return "New UI icon - can be replaced by custom icon"
                }
            }
            is FileTypeNode -> {
                return "Icon path: ${userObject.path}"
            }
            is CategoryNode -> {
                return null
            }
        }
        return null
    }

    fun excluded(): Set<String> {
        val excluded = mutableSetOf<String>()
        
        for (i in 0 until root.childCount) {
            val categoryNode = root.getChildAt(i) as? CategoryNode ?: continue
            
            for (j in 0 until categoryNode.childCount) {
                val subCategoryNode = categoryNode.getChildAt(j) as? CategoryNode ?: continue
                
                for (k in 0 until subCategoryNode.childCount) {
                    val pathNode = subCategoryNode.getChildAt(k) as? IconPathNode ?: continue
                    
                    if (pathNode.isFileType) {
                        if (!pathNode.classic) {
                            for (l in 0 until pathNode.childCount) {
                                val fileTypeNode = pathNode.getChildAt(l) as? FileTypeNode ?: continue
                                excluded.add(fileTypeNode.fileType)
                            }
                        } else {
                            for (l in 0 until pathNode.childCount) {
                                val fileTypeNode = pathNode.getChildAt(l) as? FileTypeNode ?: continue
                                if (!fileTypeNode.classic) {
                                    excluded.add(fileTypeNode.fileType)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return excluded
    }

    fun setAllClassic(pathNode: IconPathNode, classic: Boolean) {
        pathNode.classic = classic
        for (i in 0 until pathNode.childCount) {
            val child = pathNode.getChildAt(i) as? FileTypeNode ?: continue
            child.classic = classic
        }
        reload(pathNode)
    }

    fun updateFromChildren(pathNode: IconPathNode) {
        val allClassic = (0 until pathNode.childCount).all { childIndex ->
            val child = pathNode.getChildAt(childIndex) as? FileTypeNode
            child?.classic ?: true
        }
        val noneClassic = (0 until pathNode.childCount).none { childIndex ->
            val child = pathNode.getChildAt(childIndex) as? FileTypeNode
            child?.classic ?: false
        }
        
        pathNode.classic = when {
            allClassic -> true
            noneClassic -> false
            else -> false
        }
        reload(pathNode)
    }

    fun updateScope(scope: IconScope) {
        val root = super.getRoot() as DefaultMutableTreeNode
        
        for (i in 0 until root.childCount) {
            val categoryNode = root.getChildAt(i) as? CategoryNode ?: continue
            
            for (j in 0 until categoryNode.childCount) {
                val subCategoryNode = categoryNode.getChildAt(j) as? CategoryNode ?: continue
                
                for (k in 0 until subCategoryNode.childCount) {
                    val pathNode = subCategoryNode.getChildAt(k) as? IconPathNode ?: continue
                    
                    val shouldBeClassic = shouldShowClassic(
                        pathNode.path, 
                        scope, 
                        pathNode.hasClassicEquivalent
                    )
                    
                    pathNode.classic = shouldBeClassic
                    for (l in 0 until pathNode.childCount) {
                        val fileTypeNode = pathNode.getChildAt(l) as? FileTypeNode ?: continue
                        fileTypeNode.classic = pathNode.classic
                    }
                }
            }
        }
        
        reload()
    }

    fun applyFilter(filterText: String) {
        if (filterText.isBlank()) {
            val settings = ClassicIconsSettings.getInstance().state
            load(settings.excludedFileTypes, javaClass.classLoader, settings.scope)
            setCustomIconsDir(settings.customIconsDir)
            return
        }

        val pattern = Regex("(?i)".plus(Regex.escape(filterText)))
        val root = super.getRoot() as DefaultMutableTreeNode
        
        val state = mutableMapOf<String, Boolean>()
        saveState(root, state)
        
        root.removeAllChildren()
        
        val allIconsRoot = CategoryNode("All Icons")
        
        val groupedByCategory = allIcons.groupBy { 
            AllIconsScanner.getCategoryForPath(it.path) 
        }
        
        for ((category, icons) in groupedByCategory.entries.sortedBy { it.key }) {
            val categoryNode = CategoryNode(category)
            
            val groupedByPath = icons.groupBy { it.path }
            
            for ((path, pathIcons) in groupedByPath.entries.sortedBy { it.key }) {
                val firstIcon = pathIcons.first()
                val pathMatches = pattern.containsMatchIn(path) || 
                                 firstIcon.extensions.any { pattern.containsMatchIn(it) }
                
                if (firstIcon.isFileType) {
                    val matchingFileTypes = pathIcons.flatMap { it.fileTypes }.filter { 
                        pattern.containsMatchIn(it) 
                    }
                    
                    if (pathMatches || matchingFileTypes.isNotEmpty()) {
                        val allExcluded = pathIcons.all { entry -> 
                            entry.fileTypes.all { ft -> state["$path|$ft"] ?: false } 
                        }
                        
                        val shouldBeClassic = shouldShowClassic(
                            path, 
                            ClassicIconsSettings.getInstance().state.scope,
                            firstIcon.hasClassicEquivalent
                        )
                        
                        val pathNode = IconPathNode(
                            path = path,
                            icon = firstIcon.icon,
                            isFileType = true,
                            fileTypes = pathIcons.flatMap { it.fileTypes }.toSet(),
                            extensions = pathIcons.flatMap { it.extensions }.toSet(),
                            hasClassicEquivalent = firstIcon.hasClassicEquivalent,
                            isExpuiIcon = firstIcon.isExpuiIcon,
                            classic = !(allExcluded || matchingFileTypes.all { state["$path|$it"] ?: false }) && shouldBeClassic
                        )
                        
                        for (fileType in matchingFileTypes.sorted()) {
                            val isExcluded = state["$path|$fileType"] ?: false
                            val fileTypeNode = FileTypeNode(fileType, path, "", !isExcluded)
                            pathNode.add(fileTypeNode)
                        }
                        
                        categoryNode.add(pathNode)
                    }
                } else {
                    if (pathMatches) {
                        val shouldBeClassic = shouldShowClassic(
                            path, 
                            ClassicIconsSettings.getInstance().state.scope,
                            firstIcon.hasClassicEquivalent
                        )
                        val classic = (state[path] ?: shouldBeClassic) && shouldBeClassic
                        
                        val pathNode = IconPathNode(
                            path = path,
                            icon = firstIcon.icon,
                            isFileType = false,
                            fileTypes = emptySet(),
                            extensions = emptySet(),
                            hasClassicEquivalent = firstIcon.hasClassicEquivalent,
                            isExpuiIcon = firstIcon.isExpuiIcon,
                            classic = classic
                        )
                        
                        categoryNode.add(pathNode)
                    }
                }
            }
            
            if (categoryNode.childCount > 0) {
                allIconsRoot.add(categoryNode)
            }
        }
        
        if (allIconsRoot.childCount > 0) {
            root.add(allIconsRoot)
        }
        
        reload()
    }

    private fun saveState(node: DefaultMutableTreeNode, state: MutableMap<String, Boolean>) {
        when (val userObject = node.userObject) {
            is IconPathNode -> {
                state[userObject.path] = userObject.classic
                for (i in 0 until node.childCount) {
                    val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
                    saveState(child, state)
                }
            }
            is FileTypeNode -> {
                state["${userObject.path}|${userObject.fileType}"] = userObject.classic
            }
        }
    }
}
