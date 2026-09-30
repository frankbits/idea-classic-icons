package dev.frankbits.classicicons

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import java.awt.Dimension
import java.util.regex.Pattern
import javax.swing.RowFilter
import javax.swing.event.DocumentEvent
import javax.swing.table.TableRowSorter

class TooltipTable(model: FileTypeTableModel) : JBTable(model) {
    override fun getToolTipText(event: java.awt.event.MouseEvent?): String? {
        if (event == null) return null
        val viewRow = rowAtPoint(event.point)
        val viewCol = columnAtPoint(event.point)
        if (viewRow >= 0 && viewCol == 1) {
            val modelRow = convertRowIndexToModel(viewRow)
            return (model as FileTypeTableModel).getIconTooltip(modelRow)
        }
        return null
    }
}

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
        val table = TooltipTable(tableModel).apply {
            val sorter = TableRowSorter(tableModel)
            rowSorter = sorter
            columnModel.getColumn(0).maxWidth = 70
            columnModel.getColumn(1).maxWidth = 50
            columnModel.getColumn(3).maxWidth = 100
            preferredScrollableViewportSize = Dimension(600, 220)

            // Auto-resize columns based on content
            autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS

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
                row { comment("Detected from the registered file types. Untick a file type to keep its New UI icon. Filter by file type name, extension, or icon path.") }
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
        ClassicIconPatcher.refreshUi()
    }

    override fun reset() {
        super.reset()
        tableModel.load(settings.excludedFileTypes)
        tableModel.setCustomIconsDir(settings.customIconsDir)
    }
}
