package dev.frankbits.classicicons

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBTreeTable
import com.intellij.ui.dsl.builder.*
import java.awt.Dimension
import javax.swing.JScrollPane
import javax.swing.event.DocumentEvent

class ClassicIconsConfigurable : BoundConfigurable("Classic Icons") {
    private val settings get() = ClassicIconsSettings.getInstance().state

    private val treeModel by lazy {
        FileTypeIcons.refresh()
        FileTypeTreeModel().also {
            it.load(settings.excludedFileTypes, javaClass.classLoader, settings.scope)
            it.setCustomIconsDir(settings.customIconsDir)
        }
    }

    private val treeTableModel by lazy {
        IconTreeTableModel(treeModel)
    }

    private val treeTable by lazy {
        JBTreeTable(treeTableModel).apply {
            setShowsRootHandles(true)
            setRootVisible(false)
            preferredViewportSize = Dimension(600, 500)
            
            // Configure columns
            val classicColumn = getColumnModel().getColumn(0)
            classicColumn.maxWidth = 60
            classicColumn.minWidth = 60
            classicColumn.preferredWidth = 60
            
            val iconColumn = getColumnModel().getColumn(1)
            iconColumn.maxWidth = 30
            iconColumn.minWidth = 30
            iconColumn.preferredWidth = 30
            
            val pathColumn = getColumnModel().getColumn(2)
            pathColumn.preferredWidth = 300
            pathColumn.minWidth = 100
            
            val fileTypesColumn = getColumnModel().getColumn(3)
            fileTypesColumn.preferredWidth = 150
            fileTypesColumn.minWidth = 100
            
            val extensionsColumn = getColumnModel().getColumn(4)
            extensionsColumn.preferredWidth = 100
            extensionsColumn.minWidth = 80
            
            // Enable row selection
            setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION)
            
            // Set custom renderer for the Icon column
            getColumnModel().getColumn(1).cellRenderer = IconCellRenderer(treeModel)
            getColumnModel().getColumn(0).cellRenderer = CheckboxCellRenderer(treeModel)
        }
    }

    private val filterField = JBTextField()

    override fun createPanel(): DialogPanel {
        return panel {
            buttonsGroup("Icons:") {
                row { radioButton("Don't use classic icons (New UI)", IconScope.DISABLED) }
                row { radioButton("Classic icons for everything", IconScope.ALL) }
                row { radioButton("Classic icons only for files and folders", IconScope.FILES_AND_FOLDERS) }
            }.bind(settings::scope).apply {
                addChangeListener {
                    treeModel.updateScope(settings.scope)
                }
            }

            group("All icons") {
                row("Filter:") { 
                    cell(filterField).align(AlignX.FILL)
                    
                    filterField.document.addDocumentListener(object : DocumentAdapter() {
                        override fun textChanged(e: DocumentEvent) {
                            treeModel.applyFilter(filterField.text.trim())
                        }
                    })
                }
                row { 
                    cell(JScrollPane(treeTable)).align(Align.FILL)
                }
                row { 
                    comment("All icons that can be replaced. Icons with a classic equivalent can be toggled. " +
                            "Icons without classic equivalent are listed for reference and can be replaced by custom icons. " +
                            "Custom icons are indicated by an orange border.") 
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
                        "Mirror the icon paths from the tree above, e.g. <code>icons/run.svg</code> or " +
                            "<code>fileTypes/rust.svg</code>. Files in this folder win over everything else. " +
                            "SVG or PNG. Custom icons can replace ANY icon, including those without a classic equivalent."
                    )
                }
            }
        }
    }

    override fun isModified(): Boolean = 
        super.isModified() || treeModel.excluded() != settings.excludedFileTypes.toSet()

    override fun apply() {
        super.apply()
        settings.excludedFileTypes = treeModel.excluded().toMutableList()
        treeModel.setCustomIconsDir(settings.customIconsDir)
        ClassicIconPatcher.refreshUi()
    }

    override fun reset() {
        super.reset()
        treeModel.load(settings.excludedFileTypes, javaClass.classLoader, settings.scope)
        treeModel.setCustomIconsDir(settings.customIconsDir)
    }
}
