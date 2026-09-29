package dev.frankbits.classicicons

import com.intellij.openapi.util.IconLoader
import java.awt.Color
import java.io.File
import javax.swing.Icon
import javax.swing.table.AbstractTableModel

class FileTypeTableModel : AbstractTableModel() {
    class Row(
        val name: String,
        val icon: Icon,
        val path: String,
        val extensions: String,
        var classic: Boolean,
        var customPathFilter: String = ""
    )

    private var rows: List<Row> = emptyList()
    private var customIconsDir: String = ""
    private var useClassicIcons: Boolean = true

    fun load(excluded: Collection<String>, customPathFilters: Map<String, String> = emptyMap(), useClassic: Boolean = true) {
        this.useClassicIcons = useClassic
        val ex = excluded.toSet()
        rows = FileTypeIcons.entries.map { 
            Row(it.typeName, it.icon, it.path, it.extensions, it.typeName !in ex, customPathFilters[it.typeName] ?: "") 
        }
        fireTableDataChanged()
    }

    fun setUseClassic(useClassic: Boolean) {
        this.useClassicIcons = useClassic
        fireTableDataChanged()
    }

    fun setCustomIconsDir(dir: String) {
        this.customIconsDir = dir
        fireTableDataChanged()
    }

    fun excluded(): Set<String> = rows.filter { !it.classic }.map { it.name }.toSet()

    fun customPathFilters(): Map<String, String> = rows.filter { it.customPathFilter.isNotEmpty() }
        .associate { it.name to it.customPathFilter }

    /** Check if a custom icon exists for the given row */
    fun hasCustomIcon(rowIndex: Int): Boolean {
        val row = rows.getOrNull(rowIndex) ?: return false
        if (customIconsDir.isBlank()) return false
        
        val rel = row.path.removePrefix("/")
        val exact = File(customIconsDir, rel)
        if (exact.isFile) return true
        
        val base = rel.substringBeforeLast('.', rel)
        for (ext in listOf("svg", "png")) {
            val f = File(customIconsDir, "$base.$ext")
            if (f.isFile) return true
        }
        return false
    }

    /** Get the custom icon for the given row, or null if none exists */
    fun getCustomIcon(rowIndex: Int): Icon? {
        val row = rows.getOrNull(rowIndex) ?: return null
        if (customIconsDir.isBlank()) return null
        
        val rel = row.path.removePrefix("/")
        val exact = File(customIconsDir, rel)
        if (exact.isFile) {
            return IconLoader.findIcon("${exact.toURI()}", javaClass.classLoader)
        }
        
        val base = rel.substringBeforeLast('.', rel)
        for (ext in listOf("svg", "png")) {
            val f = File(customIconsDir, "$base.$ext")
            if (f.isFile) {
                return IconLoader.findIcon("${f.toURI()}", javaClass.classLoader)
            }
        }
        return null
    }

    override fun getRowCount() = rows.size
    override fun getColumnCount() = 7
    override fun getColumnName(column: Int) = arrayOf("Classic", "Icon", "File type", "Extensions", "Icon path", "Custom Icon", "Custom filter")[column]

    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        0 -> Boolean::class.javaObjectType
        1 -> Icon::class.java
        5 -> Icon::class.java
        else -> String::class.java
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = when (columnIndex) {
        0 -> true
        6 -> true
        else -> false
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any = rows[rowIndex].let {
        when (columnIndex) {
            0 -> it.classic
            1 -> getBaseIcon(rowIndex)
            2 -> it.name
            3 -> it.extensions
            4 -> it.path
            5 -> getCustomIcon(rowIndex)
            else -> it.customPathFilter
        }
    }

    /** Get the base icon (classic or expui) based on checkbox state, NOT custom icons */
    private fun getBaseIcon(rowIndex: Int): Icon {
        val row = rows[rowIndex]
        if (!useClassicIcons) return row.icon
        if (!row.classic) return row.icon
        return row.icon
    }

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex !in listOf(0, 6)) return
        
        when (columnIndex) {
            0 -> {
                rows[rowIndex].classic = value as? Boolean ?: return
                fireTableCellUpdated(rowIndex, columnIndex)
            }
            6 -> {
                rows[rowIndex].customPathFilter = value as? String ?: ""
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }
    }
}
