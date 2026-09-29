package dev.frankbits.classicicons

import javax.swing.Icon
import javax.swing.table.AbstractTableModel

class FileTypeTableModel : AbstractTableModel() {
<<<<<<< HEAD
    class Row(val name: String, val icon: Icon, val path: String, val extension: String, var classic: Boolean)
=======
    class Row(
        val name: String,
        val icon: Icon,
        val path: String,
        val extensions: String,
        var classic: Boolean,
        var customPathFilter: String = ""
    )
>>>>>>> 8ca4f33 (Add custom path filters column with per-file-type filters)

    private var rows: List<Row> = emptyList()
    private var useClassicIcons: Boolean = true

    fun load(excluded: Collection<String>, customPathFilters: Map<String, String> = emptyMap(), useClassic: Boolean = true) {
        this.useClassicIcons = useClassic
        val ex = excluded.toSet()
<<<<<<< HEAD
        rows = FileTypeIcons.entries.map { Row(it.typeName, it.icon, it.path, it.extension, it.typeName !in ex) }
=======
        rows = FileTypeIcons.entries.map { 
            Row(it.typeName, it.icon, it.path, it.extensions, it.typeName !in ex, customPathFilters[it.typeName] ?: "") 
        }
>>>>>>> 8ca4f33 (Add custom path filters column with per-file-type filters)
        fireTableDataChanged()
    }

    fun setUseClassic(useClassic: Boolean) {
        this.useClassicIcons = useClassic
        fireTableDataChanged()
    }

    fun excluded(): Set<String> = rows.filter { !it.classic }.map { it.name }.toSet()

    fun customPathFilters(): Map<String, String> = rows.filter { it.customPathFilter.isNotEmpty() }
        .associate { it.name to it.customPathFilter }

    override fun getRowCount() = rows.size
<<<<<<< HEAD
    override fun getColumnCount() = 5
    override fun getColumnName(column: Int) = arrayOf("Classic", "Icon", "File type", "Extension", "Icon path")[column]
=======
    override fun getColumnCount() = 6
    override fun getColumnName(column: Int) = arrayOf("Classic", "Icon", "File type", "Extensions", "Icon path", "Custom filter")[column]
>>>>>>> 8ca4f33 (Add custom path filters column with per-file-type filters)

    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        0 -> Boolean::class.javaObjectType
        1 -> Icon::class.java
        else -> String::class.java
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = when (columnIndex) {
        0 -> true
        5 -> true
        else -> false
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any = rows[rowIndex].let {
        when (columnIndex) {
            0 -> it.classic
            1 -> getEffectiveIcon(rowIndex)
            2 -> it.name
<<<<<<< HEAD
            3 -> it.extension
            else -> it.path
=======
            3 -> it.extensions
            4 -> it.path
            else -> it.customPathFilter
>>>>>>> 8ca4f33 (Add custom path filters column with per-file-type filters)
        }
    }

    private fun getEffectiveIcon(rowIndex: Int): Icon {
        val row = rows[rowIndex]
        if (!useClassicIcons) return row.icon
        if (!row.classic) return row.icon
        return row.icon
    }

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex !in listOf(0, 5)) return
        
        when (columnIndex) {
            0 -> {
                rows[rowIndex].classic = value as? Boolean ?: return
                fireTableCellUpdated(rowIndex, columnIndex)
            }
            5 -> {
                rows[rowIndex].customPathFilter = value as? String ?: ""
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }
    }
}
