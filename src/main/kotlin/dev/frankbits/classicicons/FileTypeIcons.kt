package dev.frankbits.classicicons

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.icons.IconPathProvider
import javax.swing.Icon

/**
 * Erkennt Datei-Icons nach Verwendung: Jeder registrierte Dateityp liefert sein Icon; dessen
 * Original-Pfad (IconPathProvider.originalPath) ist genau der Pfad, den der Patcher später sieht.
 */
object FileTypeIcons {
    data class Entry(val typeName: String, val icon: Icon, val path: String, val extension: String)

    @Volatile
    var entries: List<Entry> = emptyList()
        private set

    @Volatile
    private var byPath: Map<String, Set<String>> = emptyMap()

    fun typesFor(path: String): Set<String>? = byPath[path]

    /** @return true, wenn sich etwas geändert hat */
    fun refresh(): Boolean {
        val found = FileTypeManager.getInstance().registeredFileTypes.mapNotNull { type ->
            val icon = type.icon ?: return@mapNotNull null
            val path = (icon as? IconPathProvider)?.originalPath ?: return@mapNotNull null
            val extension = type.defaultExtension ?: ""
            Entry(type.name, icon, "/" + path.removePrefix("/"), extension)
        }.sortedBy { it.typeName.lowercase() }

        if (found.map { it.typeName to it.path } == entries.map { it.typeName to it.path }) return false
        entries = found
        byPath = found.groupBy({ it.path }, { it.typeName }).mapValues { it.value.toSet() }
        return true
    }
}
