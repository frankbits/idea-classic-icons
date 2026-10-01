package dev.frankbits.classicicons

import java.io.File
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

/**
 * Baumstruktur für alle Icons, die durch den ClassicIconPatcher ersetzt werden können.
 * 
 * Struktur:
 * - Wurzel
 *   - File Types (nur im Modus FILES_AND_FOLDERS relevant)
 *     - Icon-Pfad (z.B. /fileTypes/java.svg)
 *       - FileType (z.B. Java)
 *   - All Icons (alle anderen Icons)
 *     - Icon-Pfad (z.B. /icons/MarkdownPlugin.svg)
 *   - Custom Icons (aus dem Custom Icon Pack Verzeichnis)
 *     - Icon-Pfad
 */
class FileTypeTreeModel : DefaultTreeModel(DefaultMutableTreeNode("Icons")) {
    private var customIconsDir: String = ""
    private val root get() = super.getRoot() as DefaultMutableTreeNode
    
    // Originale Daten für Filter-Reset
    private var allIcons: List<AllIconsScanner.IconInfo> = emptyList()
    private var fileTypeIcons: List<AllIconsScanner.IconInfo> = emptyList()
    private var otherIcons: List<AllIconsScanner.IconInfo> = emptyList()

    class IconPathNode(
        val path: String,
        val icon: Icon?,
        val isFileType: Boolean,
        val fileTypes: Set<String>,
        val extensions: Set<String>,
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

    fun load(excluded: Collection<String>, classLoader: ClassLoader) {
        val ex = excluded.toSet()
        root.removeAllChildren()

        // Scanne alle Icons
        val allIconsList = AllIconsScanner.scanAllAvailableIcons(classLoader)
        allIcons = allIconsList
        
        // Trenne in FileType-Icons und andere Icons
        fileTypeIcons = allIconsList.filter { it.isFileType }
        otherIcons = allIconsList.filter { !it.isFileType }

        // Gruppiere nach Kategorien
        val categorizedOther = AllIconsScanner.groupByCategory(otherIcons)

        // 1. File Types Kategorie
        val fileTypesCategory = CategoryNode("File Types")
        
        // Gruppiere FileType-Icons nach Pfad
        val groupedFileTypes = fileTypeIcons.groupBy { it.path }.map { (path, entries) ->
            val allExcluded = entries.all { it.fileTypes.any { ft -> ft in ex } }
            val extSet = entries.flatMap { it.extensions }.toSet()
            val fileTypeSet = entries.flatMap { it.fileTypes }.toSet()
            path to Quadruple(entries.firstOrNull()?.icon, extSet, fileTypeSet, !allExcluded)
        }.sortedBy { it.first }

        for ((path, data) in groupedFileTypes) {
            val (icon, extensions, fileTypes, classic) = data
            val pathNode = IconPathNode(path, icon, true, fileTypes, extensions, classic)
            
            // Füge alle FileTypes als Kinder hinzu
            val entries = fileTypeIcons.filter { it.path == path }
            for (entry in entries.sortedBy { it.fileTypes.firstOrNull() ?: "" }) {
                for (fileType in entry.fileTypes) {
                    val isExcluded = fileType in ex
                    val fileTypeNode = FileTypeNode(fileType, path, entry.extensions.firstOrNull() ?: "", !isExcluded)
                    pathNode.add(fileTypeNode)
                }
            }
            
            fileTypesCategory.add(pathNode)
        }
        
        if (fileTypesCategory.childCount > 0) {
            root.add(fileTypesCategory)
        }

        // 2. Andere Icons nach Kategorien
        for ((category, icons) in categorizedOther.entries.sortedBy { it.key }) {
            val categoryNode = CategoryNode(category)
            
            for (iconInfo in icons.sortedBy { it.path }) {
                // Prüfe ob dieses Icon durch Custom Icon überschrieben wird
                val hasCustomIcon = if (customIconsDir.isNotBlank()) {
                    val rel = iconInfo.path.removePrefix("/")
                    val exact = File(customIconsDir, rel)
                    exact.isFile || File(customIconsDir, rel.substringBeforeLast('.', rel) + ".svg").isFile ||
                    File(customIconsDir, rel.substringBeforeLast('.', rel) + ".png").isFile
                } else {
                    false
                }
                
                val pathNode = IconPathNode(
                    path = iconInfo.path,
                    icon = iconInfo.icon,
                    isFileType = false,
                    fileTypes = emptySet(),
                    extensions = emptySet(),
                    classic = true // Standardmäßig aktiviert
                )
                
                categoryNode.add(pathNode)
            }
            
            if (categoryNode.childCount > 0) {
                root.add(categoryNode)
            }
        }

        reload()
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
        if (hasCustomIcon(node)) return "Overridden by custom icon"
        return null
    }

    fun excluded(): Set<String> {
        val excluded = mutableSetOf<String>()
        
        for (i in 0 until root.childCount) {
            val categoryNode = root.getChildAt(i) as? CategoryNode ?: continue
            
            for (j in 0 until categoryNode.childCount) {
                val pathNode = categoryNode.getChildAt(j) as? IconPathNode ?: continue
                
                if (pathNode.isFileType) {
                    // FileType-Icons
                    if (!pathNode.classic) {
                        // Alle FileTypes unter diesem Pfad sind ausgeschlossen
                        for (k in 0 until pathNode.childCount) {
                            val fileTypeNode = pathNode.getChildAt(k) as? FileTypeNode ?: continue
                            excluded.add(fileTypeNode.fileType)
                        }
                    } else {
                        // Individuelle FileTypes prüfen
                        for (k in 0 until pathNode.childCount) {
                            val fileTypeNode = pathNode.getChildAt(k) as? FileTypeNode ?: continue
                            if (!fileTypeNode.classic) {
                                excluded.add(fileTypeNode.fileType)
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
            val child = pathNode.getChildAt(childIndex) as? FileTypeNode ?: return@all
            child.classic
        }
        val noneClassic = (0 until pathNode.childCount).none { childIndex ->
            val child = pathNode.getChildAt(childIndex) as? FileTypeNode ?: return@none
            child.classic
        }
        
        pathNode.classic = when {
            allClassic -> true
            noneClassic -> false
            else -> false // Gemischt - auf false setzen
        }
        reload(pathNode)
    }

    /**
     * Filtert den Baum basierend auf dem Suchtext.
     */
    fun applyFilter(filterText: String) {
        if (filterText.isBlank()) {
            // Reset: alle Daten neu laden
            // Wir müssen die ClassLoader Referenz behalten...
            // Für jetzt einfach neu laden mit leeren Excluded
            load(emptySet(), javaClass.classLoader)
            return
        }

        val pattern = Regex("(?i)".plus(Regex.escape(filterText)))
        val root = super.getRoot() as DefaultMutableTreeNode
        
        // Speichere den Zustand der Checkboxen
        val state = mutableMapOf<String, Boolean>()
        for (i in 0 until root.childCount) {
            val categoryNode = root.getChildAt(i) as? CategoryNode ?: continue
            for (j in 0 until categoryNode.childCount) {
                val pathNode = categoryNode.getChildAt(j) as? IconPathNode ?: continue
                state[pathNode.path] = pathNode.classic
                for (k in 0 until pathNode.childCount) {
                    val fileTypeNode = pathNode.getChildAt(k) as? FileTypeNode ?: continue
                    state["${pathNode.path}|${fileTypeNode.fileType}"] = fileTypeNode.classic
                }
            }
        }
        
        // Filter anwenden
        root.removeAllChildren()
        
        // File Types
        val fileTypesCategory = CategoryNode("File Types")
        for ((path, data) in allIcons.filter { it.isFileType }.groupBy { it.path }) {
            val (icon, extensions, fileTypes, _) = data.firstOrNull()?.let {
                Quadruple(it.icon, it.extensions, it.fileTypes, true)
            } ?: continue
            
            val pathMatches = pattern.containsMatchIn(path) || extensions.any { pattern.containsMatchIn(it) }
            val matchingFileTypes = fileTypes.filter { pattern.containsMatchIn(it) }
            
            if (pathMatches || matchingFileTypes.isNotEmpty()) {
                val allExcluded = fileTypes.all { state["$path|$it"] ?: false }
                val pathNode = IconPathNode(path, icon, true, fileTypes, extensions, !allExcluded)
                
                for (fileType in matchingFileTypes.sorted()) {
                    val isExcluded = state["$path|$fileType"] ?: false
                    val fileTypeNode = FileTypeNode(fileType, path, "", !isExcluded)
                    pathNode.add(fileTypeNode)
                }
                
                fileTypesCategory.add(pathNode)
            }
        }
        
        if (fileTypesCategory.childCount > 0) {
            root.add(fileTypesCategory)
        }
        
        // Andere Icons
        val categorizedOther = AllIconsScanner.groupByCategory(otherIcons)
        for ((category, icons) in categorizedOther.entries.sortedBy { it.key }) {
            val categoryNode = CategoryNode(category)
            
            for (iconInfo in icons) {
                val pathMatches = pattern.containsMatchIn(iconInfo.path)
                
                if (pathMatches) {
                    val classic = state[iconInfo.path] ?: true
                    val pathNode = IconPathNode(
                        path = iconInfo.path,
                        icon = iconInfo.icon,
                        isFileType = false,
                        fileTypes = emptySet(),
                        extensions = emptySet(),
                        classic = classic
                    )
                    categoryNode.add(pathNode)
                }
            }
            
            if (categoryNode.childCount > 0) {
                root.add(categoryNode)
            }
        }
        
        reload()
    }
}
