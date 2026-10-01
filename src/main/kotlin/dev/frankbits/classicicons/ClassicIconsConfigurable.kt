package dev.frankbits.classicicons

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class CheckboxTree(private val treeModel: FileTypeTreeModel) : JTree(treeModel) {
    init {
        cellRenderer = FileTypeTreeCellRenderer(treeModel)
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        isRootVisible = false
        showsRootHandles = true
        preferredScrollableViewportSize = Dimension(600, 500)
        
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val path = getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                
                val bounds = getPathBounds(path) ?: return
                val checkboxWidth = 20
                
                if (e.x - bounds.x <= checkboxWidth) {
                    toggleNodeSelection(path, node)
                }
            }
        })
    }
    
    private fun toggleNodeSelection(path: TreePath, node: DefaultMutableTreeNode) {
        when (val userObject = node.userObject) {
            is FileTypeTreeModel.IconPathNode -> {
                if (userObject.hasClassicEquivalent) {
                    userObject.classic = !userObject.classic
                    treeModel.setAllClassic(userObject, userObject.classic)
                }
            }
            is FileTypeTreeModel.FileTypeNode -> {
                userObject.classic = !userObject.classic
                val parent = node.parent as? FileTypeTreeModel.IconPathNode ?: return
                treeModel.updateFromChildren(parent)
            }
            is FileTypeTreeModel.CategoryNode -> {
                val newState = false
                for (i in 0 until node.childCount) {
                    val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
                    when (val childUserObject = child.userObject) {
                        is FileTypeTreeModel.CategoryNode -> {
                            toggleCategoryChildren(child, newState)
                        }
                        is FileTypeTreeModel.IconPathNode -> {
                            if (childUserObject.hasClassicEquivalent) {
                                childUserObject.classic = newState
                                treeModel.setAllClassic(childUserObject, newState)
                            }
                        }
                    }
                }
            }
        }
        repaint()
    }
    
    private fun toggleCategoryChildren(node: DefaultMutableTreeNode, newState: Boolean) {
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
            when (val childUserObject = child.userObject) {
                is FileTypeTreeModel.CategoryNode -> {
                    toggleCategoryChildren(child, newState)
                }
                is FileTypeTreeModel.IconPathNode -> {
                    if (childUserObject.hasClassicEquivalent) {
                        childUserObject.classic = newState
                        treeModel.setAllClassic(childUserObject, newState)
                    }
                }
            }
        }
    }
}

class ClassicIconsConfigurable : BoundConfigurable("Classic Icons") {
    private val settings get() = ClassicIconsSettings.getInstance().state

    private val treeModel by lazy {
        FileTypeIcons.refresh()
        FileTypeTreeModel().also {
            it.load(settings.excludedFileTypes, javaClass.classLoader, settings.scope)
            it.setCustomIconsDir(settings.customIconsDir)
        }
    }

    private val tree by lazy {
        CheckboxTree(treeModel)
    }

    private val filterField = JBTextField()

    override fun createPanel(): DialogPanel {
        return panel {
            buttonsGroup("Icons:") {
                row { radioButton("Don't use classic icons (New UI)", IconScope.DISABLED) }
                row { radioButton("Classic icons for everything", IconScope.ALL) }
                row { radioButton("Classic icons only for files and folders", IconScope.FILES_AND_FOLDERS) }
            }.bind(settings::scope)

            group("All icons") {
                row("Filter:") { 
                    cell(filterField).align(AlignX.FILL)
                    
                    filterField.document.addDocumentListener(object : DocumentAdapter() {
                        override fun textChanged(e: DocumentEvent) {
                            treeModel.applyFilter(filterField.text.trim())
                        }
                    })
                }
                row { scrollCell(tree).align(Align.FILL) }
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
