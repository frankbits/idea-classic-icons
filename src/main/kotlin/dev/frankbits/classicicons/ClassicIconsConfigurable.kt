package dev.frankbits.classicicons

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ThreeStateCheckBox
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.Icon
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.table.DefaultTableCellRenderer

/** Table model for the entries displayed inside one source-specific group. */
private class GroupTableModel(
    private val entries: List<IconTableModel.Entry>,
    private val hasFileTypes: Boolean,
    private val groupName: String,
    private val model: IconTableModel,
    private val changed: () -> Unit
) : javax.swing.table.AbstractTableModel() {
    /** Returns the number of icon records in this group. */
    override fun getRowCount() = entries.size

    /** Returns the compact or FileType-aware column count for this group. */
    override fun getColumnCount() = if (hasFileTypes) 5 else 3

    /** Returns labels for the columns shown in this group. */
    override fun getColumnName(column: Int) =
        if (hasFileTypes) arrayOf("Classic", "Icon", "Icon path", "File types", "Extensions")[column]
        else arrayOf("Classic", "Icon", "Icon path")[column]

    /** Supplies Swing with renderer types for the group columns. */
    override fun getColumnClass(column: Int): Class<*> = when (column) {
        0 -> Boolean::class.javaObjectType
        1 -> Icon::class.java
        else -> String::class.java
    }

    /** Only the per-entry classic checkbox is editable. */
    override fun isCellEditable(row: Int, column: Int) = column == 0

    /** Returns the display value for a group entry cell. */
    override fun getValueAt(row: Int, column: Int): Any {
        val entry = entries[row]
        val path = entry.path.removePrefix("/$groupName/")
        return when (column) {
            0 -> model.entryIsClassic(entry)
            1 -> entry.icon ?: javax.swing.ImageIcon()
            2 -> path
            3 -> if (hasFileTypes) entry.fileTypes.joinToString(", ") { it.typeName } else ""
            4 -> if (hasFileTypes) entry.fileTypes.map { it.extension }
                .filter { it.isNotEmpty() }.distinct().joinToString(", ") else ""
            else -> ""
        }
    }

    /** Persists a per-entry checkbox change through the shared table model. */
    override fun setValueAt(value: Any?, row: Int, column: Int) {
        if (column == 0) {
            model.setEntryClassic(entries[row], value as? Boolean ?: return)
            changed()
        }
    }

    /** Returns whether the row has a matching custom SVG or PNG override. */
    fun hasCustomIcon(row: Int): Boolean = model.hasCustomIcon(entries[row].path)
}

/** Border and tooltip configuration for one icon group table. */
private class GroupTable(
    model: GroupTableModel,
    private val iconTooltip: (Int) -> String?
) : JBTable(model) {
    init {
        tableHeader.reorderingAllowed = false
        gridColor = javax.swing.UIManager.getColor("Table.gridColor") ?: Gray._75
        intercellSpacing = Dimension(1, 1)
    }

    /** Provides the custom-icon tooltip for the icon under the mouse. */
    override fun getToolTipText(event: java.awt.event.MouseEvent?): String? {
        if (event == null) return null
        val row = rowAtPoint(event.point)
        return if (row >= 0 && columnAtPoint(event.point) == 1) iconTooltip(row) else null
    }
}

/** Keeps a table header and body aligned inside one bordered component. */
private class GroupTablePanel(table: GroupTable) : javax.swing.JPanel(java.awt.BorderLayout()) {
    init {
        alignmentX = javax.swing.JPanel.LEFT_ALIGNMENT
        border = javax.swing.BorderFactory.createLineBorder(table.gridColor)
        table.border = javax.swing.BorderFactory.createEmptyBorder()
        table.tableHeader.border = javax.swing.BorderFactory.createEmptyBorder()
        add(table.tableHeader, java.awt.BorderLayout.NORTH)
        add(table, java.awt.BorderLayout.CENTER)
        preferredSize = Dimension(
            600,
            table.preferredSize.height + table.tableHeader.preferredSize.height + 2
        )
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
    }
}

/**
 * Settings page for selecting classic icons, excluding individual paths and
 * configuring custom icon overrides.
 */
class ClassicIconsConfigurable : BoundConfigurable("Classic Icons") {
    private val settings get() = ClassicIconsSettings.getInstance().state

    private val tableModel by lazy {
        IconRegistry.restoreRuntimePaths(settings.cachedRuntimeIconPaths)
        IconRegistry.refresh()
        IconTableModel().also {
            it.load(settings.excludedFileTypes, settings.excludedIconPaths)
            it.setCustomIconsDir(settings.customIconsDir)
        }
    }

    /** Builds the settings form and its source-separated icon groups. */
    override fun createPanel(): DialogPanel {
        val groupsPanel = javax.swing.JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
        }

        fun rebuildGroups() {
            groupsPanel.removeAll()
            val query = filterField.text.trim().lowercase()
            var displayedSource: IconTableModel.GroupSource? = null

            tableModel.groupsSnapshot().forEach { group ->
                if (displayedSource != group.source) {
                    displayedSource = group.source
                    groupsPanel.add(JLabel(
                        when (group.source) {
                            IconTableModel.GroupSource.MANAGER -> "Registered icons"
                            IconTableModel.GroupSource.RUNTIME ->
                                "Runtime-discovered icons (list may be incomplete)"
                        }
                    ).apply {
                        alignmentX = javax.swing.JPanel.LEFT_ALIGNMENT
                        font = font.deriveFont(java.awt.Font.BOLD)
                        border = JBUI.Borders.empty(8, 0, 4, 0)
                    })
                }

                val matching = group.entries.filter { entry ->
                    query.isEmpty() || (group.name + " " + entry.path + " " +
                        entry.fileTypes.joinToString(" ") { it.typeName + " " + it.extension })
                        .lowercase().contains(query)
                }
                if (matching.isEmpty()) return@forEach

                val hasFileTypes = group.entries.any { it.fileTypes.isNotEmpty() }
                val activeEntries = group.entries.count { tableModel.entryIsClassic(it) }
                val header = javax.swing.JPanel().apply {
                    layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS)
                    alignmentX = javax.swing.JPanel.LEFT_ALIGNMENT
                }
                val check = ThreeStateCheckBox().apply {
                    state = when {
                        activeEntries == 0 -> ThreeStateCheckBox.State.NOT_SELECTED
                        activeEntries == group.entries.size -> ThreeStateCheckBox.State.SELECTED
                        else -> ThreeStateCheckBox.State.DONT_CARE
                    }
                    addActionListener {
                        tableModel.setGroupClassic(group, activeEntries < group.entries.size)
                        rebuildGroups()
                    }
                }
                val chevron = JLabel(if (group.expanded) "▾" else "▸")
                val toggle = object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent) {
                        tableModel.setGroupExpanded(group, !group.expanded)
                        rebuildGroups()
                    }
                }
                header.add(check)
                header.add(chevron)
                header.add(JLabel(group.name).apply { font = font.deriveFont(java.awt.Font.BOLD) })
                header.add(Box.createHorizontalGlue())
                header.add(JLabel("${group.entries.size} icons"))
                listOf(header, chevron).forEach { it.addMouseListener(toggle) }
                groupsPanel.add(header)

                if (group.expanded || query.isNotEmpty()) {
                    val table = GroupTable(
                        GroupTableModel(matching, hasFileTypes, group.name, tableModel) {
                            rebuildGroups()
                        },
                        { row ->
                            if (tableModel.hasCustomIcon(matching[row].path)) {
                                "Overridden by custom icon"
                            } else null
                        }
                    ).apply {
                        rowHeight = 24
                        autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
                        preferredSize = Dimension(600, rowHeight * matching.size)
                        border = javax.swing.BorderFactory.createEmptyBorder()
                        columnModel.getColumn(0).maxWidth = 70
                        columnModel.getColumn(1).maxWidth = 50
                        columnModel.getColumn(1).cellRenderer = object : DefaultTableCellRenderer() {
                            override fun getTableCellRendererComponent(
                                table: javax.swing.JTable, value: Any?, selected: Boolean,
                                focus: Boolean, row: Int, column: Int
                            ): Component {
                                val label = super.getTableCellRendererComponent(
                                    table, null, selected, focus, row, column
                                ) as JLabel
                                label.icon = value as? Icon
                                label.text = ""
                                label.horizontalAlignment = SwingConstants.CENTER
                                label.border = if ((table.model as GroupTableModel)
                                        .let { it.hasCustomIcon(row) }) {
                                    javax.swing.border.CompoundBorder(
                                        javax.swing.border.MatteBorder(
                                            0, 1, 0, 0, JBColor.ORANGE
                                        ),
                                        JBUI.Borders.empty()
                                    )
                                } else {
                                    javax.swing.BorderFactory.createEmptyBorder()
                                }
                                return label
                            }
                        }
                    }
                    groupsPanel.add(GroupTablePanel(table))
                }
            }
            groupsPanel.revalidate()
            groupsPanel.repaint()
        }

        rebuildGroups()
        filterField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = rebuildGroups()
        })

        return panel {
            buttonsGroup("Icons:") {
                row { radioButton("Don't use classic icons (New UI)", IconScope.DISABLED) }
                row { radioButton("Classic icons for everything", IconScope.ALL) }
                row { radioButton("Classic icons only for files and folders", IconScope.FILES_AND_FOLDERS) }
            }.bind(settings::scope)

            group("File types (mode \"files and folders\")") {
                row("Filter:") { cell(filterField).align(AlignX.FILL) }
                row { cell(groupsPanel).align(Align.FILL) }
                row {
                    comment("Groups are collapsed by default. Click a group row to expand or collapse it; use the checkbox to change all icons in the group.")
                }
            }

            collapsibleGroup("Advanced: additional path filters") {
                row {
                    textArea()
                        .rows(4)
                        .align(AlignX.FILL)
                        .bindText(settings::extraFilters)
                        .comment("One path fragment per line (e.g. MarkdownPlugin). Only used in \"Only files and folders\" mode.")
                }
            }

            group("Custom icon pack") {
                row("Folder:") {
                    val field = TextFieldWithBrowseButton()
                    field.addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFolderDescriptor())
                    cell(field).align(AlignX.FILL).bindText(settings::customIconsDir)
                }
                row {
                    comment(
                        "Mirror the icon paths from the table above, e.g. <code>fileTypes/java.svg</code> or " +
                            "<code>icons/MarkdownPlugin.svg</code>. Files in this folder win over everything else. SVG or PNG."
                    )
                }
            }
        }
    }

    private val filterField = JBTextField()

    /** Reports changes in both standard settings and table exclusions. */
    override fun isModified(): Boolean =
        super.isModified() ||
            tableModel.excluded() != settings.excludedFileTypes.toSet() ||
            tableModel.excludedPaths() != settings.excludedIconPaths.toSet()

    /** Persists exclusions, refreshes icon caches and rebuilds the registry view. */
    override fun apply() {
        super.apply()
        settings.excludedFileTypes = tableModel.excluded().toMutableList()
        settings.excludedIconPaths = tableModel.excludedPaths().toMutableList()
        tableModel.setCustomIconsDir(settings.customIconsDir)
        ClassicIconPatcher.refreshUi()
        IconRegistry.refresh()
        tableModel.load(settings.excludedFileTypes, settings.excludedIconPaths)
        tableModel.setCustomIconsDir(settings.customIconsDir)
    }

    /** Restores persisted values and reloads the table model. */
    override fun reset() {
        super.reset()
        tableModel.load(settings.excludedFileTypes, settings.excludedIconPaths)
        tableModel.setCustomIconsDir(settings.customIconsDir)
    }
}
