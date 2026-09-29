package dev.frankbits.classicicons

import javax.swing.Icon
import javax.swing.table.AbstractTableModel

class FileTypeTableModel : AbstractTableModel() {
    class Row(val name: String, val icon: Icon, val path: String, var classic: Boolean)

    private var rows: List<Row> = emptyList()

    fun load(excluded: Collection<String>) {
        val ex = excluded.toSet()
        rows = FileTypeIcons.entries.map { Row(it.typeName, it.icon, it.path, it.typeName !in ex) }
        fireTableDataChanged()
    }

    fun excluded(): Set<String> = rows.filter { !it.classic }.map { it.name }.toSet()

    override fun getRowCount() = rows.size
    override fun getColumnCount() = 4
    override fun getColumnName(column: Int) = arrayOf("Classic", "Icon", "File type", "Icon path")[column]

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
