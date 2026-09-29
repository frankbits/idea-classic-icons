package dev.frankbits.classicicons

import javax.swing.Icon
import javax.swing.table.AbstractTableModel

class FileTypeTableModel : AbstractTableModel() {
    class Row(val name: String, val icon: Icon, val path: String, val extension: String, var classic: Boolean)

    private var rows: List<Row> = emptyList()
    private var useClassicIcons: Boolean = true

    fun load(excluded: Collection<String>, useClassic: Boolean = true) {
        this.useClassicIcons = useClassic
        val ex = excluded.toSet()
        rows = FileTypeIcons.entries.map { Row(it.typeName, it.icon, it.path, it.extension, it.typeName !in ex) }
        fireTableDataChanged()
    }

    fun setUseClassic(useClassic: Boolean) {
        this.useClassicIcons = useClassic
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
            1 -> getEffectiveIcon(rowIndex)
            2 -> it.name
            3 -> it.extension
            else -> it.path
        }
    }

    private fun getEffectiveIcon(rowIndex: Int): Icon {
        val row = rows[rowIndex]
        if (!useClassicIcons) return row.icon
        if (!row.classic) return row.icon
        return row.icon
    }

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 0) {
            rows[rowIndex].classic = value as? Boolean ?: return
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }
}
