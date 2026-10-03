package dev.frankbits.classicicons

import java.io.File
import com.intellij.util.ui.EmptyIcon
import javax.swing.Icon
import javax.swing.table.AbstractTableModel

/**
 * Maintains grouped registry entries and the user's per-file-type/path exclusions.
 *
 * The settings UI uses this model as the single mutable view of checkbox state;
 * persistence is handled by [ClassicIconsConfigurable].
 */
class IconTableModel : AbstractTableModel() {
    /** UI grouping source derived from the registry metadata types. */
    enum class GroupSource { MANAGER, RUNTIME }

    /** Flattened table row representation retained for the legacy model API. */
    class Row(
        val path: String,
        val icon: Icon?,
        val fileTypes: String,
        val extensions: String,
        var classic: Boolean,
        val group: Group? = null,
        val entry: Entry? = null,
        val displayPath: String = path
    ) {
        /** Whether this row represents a group instead of an individual icon. */
        val isGroup get() = group != null
    }

    /** UI entry derived from one registry record. */
    class Entry(
        val path: String,
        val icon: Icon?,
        val fileTypes: List<IconRegistry.IconMetadata.FileType>,
        val source: GroupSource
    )

    /** Group of entries sharing a source and first icon-path segment. */
    class Group(
        val name: String,
        val entries: List<Entry>,
        val source: GroupSource,
        var expanded: Boolean = false
    )

    private var rows: List<Row> = emptyList()
    private var groups: List<Group> = emptyList()
    private var customIconsDir: String = ""
    private var excludedFileTypes: Set<String> = emptySet()
    private var excludedPaths: Set<String> = emptySet()
    private val expandedGroups = mutableSetOf<String>()

    private fun groupKey(source: GroupSource, name: String) = "${source.name}:$name"

    /** Rebuilds groups from the current registry while preserving expansion state. */
    fun load(excluded: Collection<String>, excludedPaths: Collection<String> = emptyList()) {
        excludedFileTypes = excluded.toSet()
        this.excludedPaths = excludedPaths.toSet()

        val byPath = IconRegistry.iconRecords().associate { record ->
            val source = if (record.metadata.any {
                    it is IconRegistry.IconMetadata.FileType ||
                    it is IconRegistry.IconMetadata.Action
                }) {
                GroupSource.MANAGER
            } else {
                GroupSource.RUNTIME
            }
            record.path to Entry(
                record.path,
                record.icon,
                record.metadata.filterIsInstance<IconRegistry.IconMetadata.FileType>(),
                source
            )
        }

        groups = byPath.values.groupBy { groupKey(it.source, groupName(it.path)) }
            .map { (name, entries) ->
                val source = entries.first().source
                val displayName = name.substringAfter(':')
                Group(
                    displayName,
                    entries.sortedBy { it.path.lowercase() },
                    source,
                    name in expandedGroups
                )
            }
            .sortedWith(compareBy<Group> { it.source }.thenBy { it.name.lowercase() })
        rebuildRows()
        fireTableDataChanged()
    }

    private fun groupName(path: String): String =
        path.removePrefix("/").substringBefore('/').ifEmpty { "Other" }

    private fun rebuildRows() {
        rows = groups.flatMap { group ->
            val groupClassic = group.entries.all { isClassic(it) }
            val fileTypeCount = group.entries.sumOf { it.fileTypes.size }
            val groupRow = Row(
                path = "${if (group.expanded) "v " else "> "}${group.name}",
                icon = null,
                fileTypes = "${group.entries.size} icons",
                extensions = if (fileTypeCount == 0) "No file types" else "$fileTypeCount file types",
                classic = groupClassic,
                group = group
            )
            val itemRows = if (group.expanded) group.entries.map { entry ->
                Row(
                    path = entry.path,
                    icon = entry.icon,
                    fileTypes = entry.fileTypes.joinToString(", ") { it.typeName }
                        .ifEmpty { "Not associated with a file type" },
                    extensions = entry.fileTypes.map { it.extension }.filter { it.isNotEmpty() }
                        .distinct().joinToString(", ").ifEmpty {
                        if (entry.fileTypes.isEmpty()) "Not applicable" else "No default extension"
                    },
                    classic = isClassic(entry),
                    entry = entry,
                    displayPath = entry.path.removePrefix("/${group.name}/")
                )
            } else emptyList()
            listOf(groupRow) + itemRows
        }
    }

    private fun isClassic(entry: Entry): Boolean =
        if (entry.fileTypes.isEmpty()) entry.path !in excludedPaths
        else entry.fileTypes.any { it.typeName !in excludedFileTypes }

    /** Returns detached group snapshots for rendering the settings page. */
    fun groupsSnapshot(): List<Group> = groups.map { group ->
        Group(group.name, group.entries.toList(), group.source, group.expanded)
    }

    /** Updates expansion state for the source-specific group identified by [group]. */
    fun setGroupExpanded(group: Group, expanded: Boolean) {
        groups.firstOrNull {
            it.source == group.source && it.name == group.name
        }?.expanded = expanded
        val key = groupKey(group.source, group.name)
        if (expanded) expandedGroups.add(key) else expandedGroups.remove(key)
    }

    /** Returns whether [entry] is currently enabled for classic replacement. */
    fun entryIsClassic(entry: Entry): Boolean = isClassic(entry)

    /** Changes the exclusion state of one registry entry. */
    fun setEntryClassic(entry: Entry, classic: Boolean) {
        if (classic) {
            excludedPaths -= entry.path
            excludedFileTypes -= entry.fileTypes.map { it.typeName }.toSet()
        } else if (entry.fileTypes.isEmpty()) {
            excludedPaths += entry.path
        } else {
            excludedFileTypes += entry.fileTypes.map { it.typeName }.toSet()
        }
        rebuildRows()
        fireTableDataChanged()
    }

    /** Applies one classic/New UI state to every entry in [group]. */
    fun setGroupClassic(group: Group, classic: Boolean) {
        group.entries.forEach { setEntryClassic(it, classic) }
    }

    /** Updates the directory used to mark custom overrides in the preview. */
    fun setCustomIconsDir(dir: String) {
        customIconsDir = dir
        fireTableDataChanged()
    }

    /** Returns whether a custom SVG/PNG override exists for [path]. */
    fun hasCustomIcon(path: String): Boolean {
        if (customIconsDir.isBlank()) return false
        val rel = path.removePrefix("/")
        val exact = File(customIconsDir, rel)
        if (exact.isFile) return true
        val base = rel.substringBeforeLast('.', rel)
        return listOf("svg", "png").any { File(customIconsDir, "$base.$it").isFile }
    }

    /** Returns excluded FileType names for persistence. */
    fun excluded(): Set<String> =
        groups.flatMap { group ->
            group.entries.flatMap { entry ->
                entry.fileTypes.filter { it.typeName in excludedFileTypes }.map { it.typeName }
            }
        }.toSet()

    /** Returns excluded non-FileType paths for persistence. */
    fun excludedPaths(): Set<String> =
        groups.flatMap { it.entries.filter { entry ->
            entry.fileTypes.isEmpty() && !isClassic(entry)
        }.map { it.path } }.toSet()

    /** Returns the flattened row count for the legacy table representation. */
    override fun getRowCount() = rows.size
    /** Returns the fixed number of columns used by the legacy table view. */
    override fun getColumnCount() = 5

    /** Returns the column labels used by the legacy table view. */
    override fun getColumnName(column: Int) =
        arrayOf("Classic", "Icon", "File types", "Extensions", "Icon path")[column]

    /** Supplies Swing with the renderer type for each column. */
    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        0 -> Boolean::class.javaObjectType
        1 -> Icon::class.java
        else -> String::class.java
    }

    /** Only the classic-icon checkbox column can be edited. */
    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = columnIndex == 0

    /** Returns the display value for one legacy table cell. */
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any = rows[rowIndex].let {
        when (columnIndex) {
            0 -> it.classic
            1 -> it.icon ?: EmptyIcon.create(16)
            2 -> it.fileTypes
            3 -> it.extensions
            else -> it.displayPath
        }
    }

    /** Applies a legacy-table checkbox change to its group or individual entry. */
    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex != 0) return
        val classic = value as? Boolean ?: return
        val row = rows[rowIndex]
        row.classic = classic
        row.group?.entries?.forEach { entry ->
            if (classic) {
                excludedPaths -= entry.path
                excludedFileTypes -= entry.fileTypes.map { it.typeName }.toSet()
            } else if (entry.fileTypes.isEmpty()) {
                excludedPaths += entry.path
            } else {
                excludedFileTypes += entry.fileTypes.map { it.typeName }.toSet()
            }
        }
        row.entry?.let { entry ->
            if (classic) {
                excludedPaths -= entry.path
                excludedFileTypes -= entry.fileTypes.map { it.typeName }.toSet()
            } else if (entry.fileTypes.isEmpty()) {
                excludedPaths += entry.path
            } else {
                excludedFileTypes += entry.fileTypes.map { it.typeName }.toSet()
            }
        }
        rebuildRows()
        fireTableDataChanged()
    }
}
