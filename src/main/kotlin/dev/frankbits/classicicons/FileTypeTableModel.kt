package dev.frankbits.classicicons

import com.intellij.openapi.util.IconLoader
import java.io.File
import javax.swing.Icon
import javax.swing.table.AbstractTableModel

class FileTypeTableModel : AbstractTableModel() {
    class Row(val name: String, val icon: Icon, val path: String, val extension: String, var classic: Boolean)

    private var rows: List<Row> = emptyList()
    private var customIconsDir: String = ""
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

    fun setCustomIconsDir(dir: String) {
        this.customIconsDir = dir
        fireTableDataChanged()
    }

    fun excluded(): Set<String> = rows.filter { !it.classic }.map { it.name }.toSet()

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

    /** Check if the icon at the given row should be greyed out.
     * Grey out when:
     * - Custom icon exists for this file type (overridden)
     * - Checkbox is unchecked (in files and folders mode)
     */
    fun isIconGreyedOut(rowIndex: Int): Boolean {
        val row = rows.getOrNull(rowIndex) ?: return false
        if (hasCustomIcon(rowIndex)) return true
        return useClassicIcons && !row.classic
    }

    /** Get the tooltip for the icon at the given row */
    fun getIconTooltip(rowIndex: Int): String? {
        val row = rows.getOrNull(rowIndex) ?: return null
        if (hasCustomIcon(rowIndex)) return "Overridden by custom icon"
        if (!useClassicIcons) return null
        if (row.classic) return null
        return "Will use New UI icon (classic disabled for this file type)"
    }

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
