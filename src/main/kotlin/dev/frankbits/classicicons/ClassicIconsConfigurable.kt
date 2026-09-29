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
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter
import javax.swing.event.DocumentEvent

class ClassicIconsConfigurable : BoundConfigurable("Classic Icons") {
    private val settings get() = ClassicIconsSettings.getInstance().state

    private val tableModel by lazy {
        FileTypeIcons.refresh()
        FileTypeTableModel().also { 
            it.load(settings.excludedFileTypes, settings.customPathFilters, settings.scope != IconScope.DISABLED)
            it.setCustomIconsDir(settings.customIconsDir)
        }
    }

    override fun createPanel(): DialogPanel {
        val table = JBTable(tableModel).apply {
            val sorter = TableRowSorter(tableModel)
            rowSorter = sorter
            columnModel.getColumn(0).maxWidth = 70
            columnModel.getColumn(1).maxWidth = 50
            columnModel.getColumn(5).maxWidth = 50
            columnModel.getColumn(6).maxWidth = 150
            preferredScrollableViewportSize = Dimension(900, 220)
            
            // Set custom renderer to grey out rows with custom icons
            val customIconColumn = 5
            setDefaultRenderer(Icon::class.java, object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable,
                    value: Any?,
                    isSelected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int
                ): Component {
                    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    
                    // Grey out the base icon column if a custom icon exists for this row
                    if (column == 1 && tableModel.hasCustomIcon(row)) {
                        comp.foreground = Color.GRAY
                        (comp as? JLabel)?.toolTipText = "Overridden by custom icon"
                    } else {
                        comp.foreground = table.foreground
                    }
                    
                    // Grey out the entire row if it has a custom icon
                    if (tableModel.hasCustomIcon(row) && column != customIconColumn) {
                        comp.foreground = Color.GRAY
                    }
                    
                    return comp
                }
            })
            
            // Set renderer for the Custom Icon column to show tooltips
            columnModel.getColumn(customIconColumn).cellRenderer = object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable,
                    value: Any?,
                    isSelected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int
                ): Component {
                    val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    if (value != null) {
                        (comp as? JLabel)?.toolTipText = "Custom icon from pack"
                    }
                    return comp
                }
            }
            
            filterField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    val text = filterField.text.trim()
                    sorter.rowFilter =
                        if (text.isEmpty()) null else RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2, 3, 4)
                }
            })
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
                row { comment("Detected from the registered file types. Untick a file type to keep its New UI icon. Rows with custom icons are greyed out. Filter by file type name, extension, or icon path.") }
            }

            collapsibleGroup("Advanced: additional path filters") {
                row {
                    cell(JBTextArea(4, 40).apply { border = BorderFactory.createLineBorder(Color.GRAY) })
                        .align(AlignX.FILL)
                        .bindText(settings::extraFilters)
                }
                row { comment("One path fragment per line, e.g. MarkdownPlugin. Only used in \"files and folders\" mode.") }
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
                            "SVG or PNG. Custom icons appear in the 'Custom Icon' column."
                    )
                }
            }
        }
    }

    private val filterField = JBTextField()

    override fun isModified(): Boolean =
        super.isModified() || tableModel.excluded() != settings.excludedFileTypes.toSet() ||
        tableModel.customPathFilters() != settings.customPathFilters

    override fun apply() {
        super.apply()
        settings.excludedFileTypes = tableModel.excluded().toMutableList()
        settings.customPathFilters = tableModel.customPathFilters().toMutableMap()
        tableModel.setUseClassic(settings.scope != IconScope.DISABLED)
        tableModel.setCustomIconsDir(settings.customIconsDir)
        ClassicIconPatcher.refreshUi()
    }

    override fun reset() {
        super.reset()
        tableModel.load(settings.excludedFileTypes, settings.customPathFilters, settings.scope != IconScope.DISABLED)
        tableModel.setCustomIconsDir(settings.customIconsDir)
        tableModel.setUseClassic(settings.scope != IconScope.DISABLED)
    }
}
