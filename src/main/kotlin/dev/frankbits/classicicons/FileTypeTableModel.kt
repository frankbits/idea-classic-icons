package dev.frankbits.classicicons

import java.io.File
import javax.swing.Icon
import javax.swing.table.AbstractTableModel

class FileTypeTableModel : AbstractTableModel() {
    class Row(val name: String, val icon: Icon, val path: String, val extension: String, var classic: Boolean)

    private var rows: List<Row> = emptyList()
    private var customIconsDir: String = ""

    fun setCustomIconsDir(dir: String) {
        this.customIconsDir = dir
        fireTableDataChanged()
    }

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

    fun getIconTooltip(rowIndex: Int): String? {
        if (hasCustomIcon(rowIndex)) return "Overridden by custom icon"
        return null
    }

    fun load(excluded: Collection<String>) {
        val ex = excluded.toSet()
        rows = FileTypeIcons.entries.map { Row(it.typeName, it.icon, it.path, it.extension, it.typeName !in ex) }
        fireTableDataChanged()
    }

    fun excluded(): Set<String> = rows.filter { !it.classic }.map { it.name }.toSet()

    override fun getRowCount() = rows.size
    override fun getColumnCount() = 5
    override fun getColumnName(column: Int) = arrayOf("Classic", "Icon", "File type", "Extension", "Icon path")[column]

    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        0 -> Boolean::class.javaObjectType
        1 -> Icon::class.java
        else -> String::class.java
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = columnIndex == 0

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any = rows[rowIndex].let {
        when (columnIndex) {
            0 -> it.classic
            1 -> it.icon
            2 -> it.name
            3 -> it.extension
            else -> it.path
        }
    }

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 0) {
            rows[rowIndex].classic = value as? Boolean ?: return
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }
}
