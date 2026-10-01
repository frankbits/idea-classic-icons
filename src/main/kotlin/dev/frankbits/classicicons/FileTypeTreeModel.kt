package dev.frankbits.classicicons

import java.io.File
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

/**
 * Baumstruktur für FileType-Icons, gruppiert nach Icon-Pfaden.
 * Jeder Icon-Pfad ist ein Knoten, darunter die zugehörigen FileTypes als Blätter.
 */
class FileTypeTreeModel : DefaultTreeModel(DefaultMutableTreeNode("Icons")) {
    private var customIconsDir: String = ""
    private val root get() = super.getRoot() as DefaultMutableTreeNode
    
    // Originale Daten für Filter-Reset
    private var originalData: List<Pair<String, Triple<Icon, String, List<FileTypeIcons.Entry>>>> = emptyList()

    class IconPathNode(
        val path: String,
        val icon: Icon,
        val extensions: String,
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

    fun load(excluded: Collection<String>) {
        val ex = excluded.toSet()
        root.removeAllChildren()

        // Gruppiere nach Icon-Pfad
        val grouped = FileTypeIcons.entries.groupBy { it.path }.map { (path, entries) ->
            val allExcluded = entries.all { it.typeName in ex }
            val extList = entries.map { it.extension }.filter { it.isNotEmpty() }.distinct().joinToString(", ")
            path to Triple(entries.first().icon, extList, entries.sortedBy { it.typeName })
        }.sortedBy { it.first }
        
        originalData = grouped

        // Erstelle Baumstruktur
        for ((path, data) in grouped) {
            val (icon, extensions, entries) = data
            val allExcluded = entries.all { it.typeName in ex }
            val pathNode = IconPathNode(path, icon, extensions, !allExcluded)
            
            // Füge alle FileTypes als Kinder hinzu
            for (entry in entries) {
                val isExcluded = entry.typeName in ex
                val fileTypeNode = FileTypeNode(entry.typeName, path, entry.extension, !isExcluded)
                pathNode.add(fileTypeNode)
            }
            
            root.add(pathNode)
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
            val pathNode = root.getChildAt(i) as? IconPathNode ?: continue
            if (!pathNode.classic) {
                // Alle FileTypes unter diesem Pfad sind ausgeschlossen
                for (j in 0 until pathNode.childCount) {
                    val fileTypeNode = pathNode.getChildAt(j) as? FileTypeNode ?: continue
                    excluded.add(fileTypeNode.fileType)
                }
            } else {
                // Individuelle FileTypes prüfen
                for (j in 0 until pathNode.childCount) {
                    val fileTypeNode = pathNode.getChildAt(j) as? FileTypeNode ?: continue
                    if (!fileTypeNode.classic) {
                        excluded.add(fileTypeNode.fileType)
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
     * Behält die Struktur bei und zeigt nur passende Knoten an.
     */
    fun applyFilter(filterText: String) {
        if (filterText.isBlank()) {
            // Reset: alle Daten neu laden
            val excluded = originalData.flatMap { it.second.third }.map { it.typeName }.toSet()
            load(excluded)
            return
        }

        val pattern = Regex("(?i)".plus(Regex.escape(filterText)))
        val root = super.getRoot() as DefaultMutableTreeNode
        
        // Speichere den Zustand der Checkboxen
        val state = mutableMapOf<String, Boolean>()
        for (i in 0 until root.childCount) {
            val pathNode = root.getChildAt(i) as? IconPathNode ?: continue
            state[pathNode.path] = pathNode.classic
            for (j in 0 until pathNode.childCount) {
                val fileTypeNode = pathNode.getChildAt(j) as? FileTypeNode ?: continue
                state["${pathNode.path}|${fileTypeNode.fileType}"] = fileTypeNode.classic
            }
        }
        
        // Filter anwenden
        root.removeAllChildren()
        
        for ((path, data) in originalData) {
            val (icon, extensions, entries) = data
            
            // Prüfe ob der Pfad selbst matcht
            val pathMatches = pattern.containsMatchIn(path) || pattern.containsMatchIn(extensions)
            
            // Filtere die FileTypes
            val matchingEntries = entries.filter { entry ->
                pattern.containsMatchIn(entry.typeName) || 
                pattern.containsMatchIn(entry.extension) || 
                pattern.containsMatchIn(path)
            }
            
            // Wenn entweder der Pfad matcht oder mindestens ein FileType
            if (pathMatches || matchingEntries.isNotEmpty()) {
                val allExcluded = entries.all { it.typeName in state.filterKeys { it.startsWith(path) }.map { it.substringAfter("|", "") }.toSet() }
                val pathNode = IconPathNode(path, icon, extensions, !allExcluded)
                
                // Nur die passenden FileTypes hinzufügen
                for (entry in matchingEntries) {
                    val isExcluded = state["$path|${entry.typeName}"] ?: false
                    val fileTypeNode = FileTypeNode(entry.typeName, path, entry.extension, !isExcluded)
                    pathNode.add(fileTypeNode)
                }
                
                // Wenn der Pfad selbst nicht matcht, aber FileTypes schon, markiere das
                if (!pathMatches && matchingEntries.isNotEmpty()) {
                    // Pfad anzeigen weil Kinder matchen
                }
                
                root.add(pathNode)
            }
        }
        
        reload()
    }
}
