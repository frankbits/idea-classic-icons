package dev.frankbits.classicicons

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.util.regex.Pattern
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.RowFilter
import javax.swing.event.DocumentEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter

class ClassicIconsConfigurable : BoundConfigurable("Classic Icons") {
    private val settings get() = ClassicIconsSettings.getInstance().state

    private val tableModel by lazy {
        FileTypeIcons.refresh()
        FileTypeTableModel().also {
            it.load(settings.excludedFileTypes)
            it.setCustomIconsDir(settings.customIconsDir)
        }
    }

    override fun createPanel(): DialogPanel {
        val table = JBTable(tableModel).apply {
            val sorter = TableRowSorter(tableModel)
            rowSorter = sorter
            columnModel.getColumn(0).maxWidth = 70
            columnModel.getColumn(1).maxWidth = 50
            columnModel.getColumn(2).minWidth = 150
            columnModel.getColumn(3).maxWidth = 100
            preferredScrollableViewportSize = Dimension(600, 220)
            
            autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
            
            filterField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    val text = filterField.text.trim()
                    sorter.rowFilter =
                        if (text.isEmpty()) null else RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2, 3, 4)
                }
            })

            columnModel.getColumn(1).cellRenderer = object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: javax.swing.JTable,
                    value: Any?,
                    isSelected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int
                ): Component {
                    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
                    comp.border = null
                    comp.horizontalAlignment = JLabel.CENTER
                    if (value is Icon) {
                        comp.icon = value
                        comp.text = ""
                    }
                    val modelRow = table.convertRowIndexToModel(row)
                    if (tableModel.hasCustomIcon(modelRow)) {
                        comp.border = javax.swing.border.CompoundBorder(
                            javax.swing.border.MatteBorder(0, 1, 0, 0, java.awt.Color.ORANGE),
                            javax.swing.border.EmptyBorder(0, 0, 0, 0)
                        )
                    } else {
                        comp.border = null
                    }
                    return comp
                }
            }
        }

        return panel {
            buttonsGroup("Icons:") {
                row { radioButton("Don't use classic icons (New UI)", IconScope.DISABLED) }
                row { radioButton("Classic icons for everything", IconScope.ALL) }
                row { radioButton("Classic icons only for files and folders", IconScope.FILES_AND_FOLDERS) }
            }.bind(settings::scope)

            group("File types (mode \"files and folders\")") {
                row("Filter:") { cell(filterField).align(AlignX.FILL) }
                row { scrollCell(table).align(Align.FILL) }
                row { comment("Detected from the registered file types. File types sharing the same icon are grouped. Untick a group to keep its New UI icon. Filter by file type name, extension, or icon path.") }
            }.apply {
                isVisible = settings.scope == IconScope.FILES_AND_FOLDERS
            }

            collapsibleGroup("Advanced: additional path filters") {
                row {
                    cell(JBTextArea(4, 40).apply { border = BorderFactory.createLineBorder(Color.GRAY) })
                        .align(AlignX.FILL)
                        .bindText(settings::extraFilters)
                }
                row { comment("One path fragment per line, e.g. MarkdownPlugin. Only used in \"files and folders\" mode.") }
            }.apply {
                isVisible = settings.scope == IconScope.FILES_AND_FOLDERS
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
                            "<code>icons/MarkdownPlugin.svg</code>. Files in this folder win over everything else. " +
                            "SVG or PNG."
                    )
                }
            }
        }
    }

    private val filterField = JBTextField()

    override fun isModified(): Boolean =
        super.isModified() || tableModel.excluded() != settings.excludedFileTypes.toSet()

    override fun apply() {
        super.apply()
        settings.excludedFileTypes = tableModel.excluded().toMutableList()
        tableModel.setCustomIconsDir(settings.customIconsDir)
        ClassicIconPatcher.refreshUi()
    }

    override fun reset() {
        super.reset()
        tableModel.load(settings.excludedFileTypes)
        tableModel.setCustomIconsDir(settings.customIconsDir)
    }
}
