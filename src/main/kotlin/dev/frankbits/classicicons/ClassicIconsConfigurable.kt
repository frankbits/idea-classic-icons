package dev.frankbits.classicicons

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.tree.JBTree
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class CheckboxTree(private val treeModel: FileTypeTreeModel) : JBTree(treeModel) {
    init {
        cellRenderer = FileTypeTreeCellRenderer(treeModel)
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        isRootVisible = false
        showsRootHandles = true
        
        // Handle checkbox clicks
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val path = getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                
                // Check if click was on the checkbox area (rough estimate)
                val bounds = getPathBounds(path) ?: return
                val checkboxWidth = 20 // Approximate checkbox width
                
                if (e.x - bounds.x <= checkboxWidth) {
                    toggleNodeSelection(path, node)
                }
            }
        })
    }
    
    private fun toggleNodeSelection(path: TreePath, node: DefaultMutableTreeNode) {
        when (val userObject = node.userObject) {
            is FileTypeTreeModel.IconPathNode -> {
                // Prüfe ob das Icon ersetzt werden kann (klassisches Äquivalent oder Custom Icon)
                val hasCustomIcon = treeModel.hasCustomIcon(node)
                val canBeReplaced = userObject.hasClassicEquivalent || hasCustomIcon
                
                if (canBeReplaced) {
                    userObject.classic = !userObject.classic
                    treeModel.setAllClassic(userObject, userObject.classic)
                }
            }
            is FileTypeTreeModel.FileTypeNode -> {
                userObject.classic = !userObject.classic
                // Update parent node state based on children
                val parent = node.parent as? FileTypeTreeModel.IconPathNode ?: return
                treeModel.updateFromChildren(parent)
            }
            is FileTypeTreeModel.CategoryNode -> {
                // Toggle all children
                val newState = !userObject.classic
                for (i in 0 until node.childCount) {
                    val child = node.getChildAt(i) as? DefaultMutableTreeNode ?: continue
                    when (val childUserObject = child.userObject) {
                        is FileTypeTreeModel.CategoryNode -> {
                            // Rekursiv alle Kinder toggeln
                            toggleCategoryChildren(child, newState)
                        }
                        is FileTypeTreeModel.IconPathNode -> {
                            val hasCustomIcon = treeModel.hasCustomIcon(child)
                            val canBeReplaced = childUserObject.hasClassicEquivalent || hasCustomIcon
                            if (canBeReplaced) {
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
                    val hasCustomIcon = treeModel.hasCustomIcon(child)
                    val canBeReplaced = childUserObject.hasClassicEquivalent || hasCustomIcon
                    if (canBeReplaced) {
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
        CheckboxTree(treeModel).apply {
            preferredScrollableViewportSize = Dimension(600, 500)
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
                // Listener für Scope-Änderungen
                addChangeListener {
                    treeModel.updateScope(settings.scope)
                    treeModel.applyFilter(filterField.text.trim())
                }
            }

            group("All icons") {
                row("Filter:") { 
                    cell(filterField).align(AlignX.FILL)
                    
                    // Add filter listener
                    filterField.document.addDocumentListener(object : DocumentAdapter() {
                        override fun textChanged(e: DocumentEvent) {
                            treeModel.applyFilter(filterField.text.trim())
                        }
                    })
                }
                row { scrollCell(tree).align(Align.FILL) }
                row { 
                    comment("All icons that can be replaced. Icons without a classic equivalent are shown in the list " +
                            "and can be replaced by custom icons. File types have their individual file types as children. " +
                            "Filter by path, file type name, or extension.") 
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
                        "Mirror the icon paths from the tree above, e.g. <code>fileTypes/java.svg</code> or " +
                            "<code>icons/MarkdownPlugin.svg</code>. Files in this folder win over everything else. " +
                            "SVG or PNG. Custom icons can replace ANY icon, including those without a classic equivalent."
                    )
                }
            }
        }
    }

    override fun isModified(): Boolean =\n        super.isModified() || treeModel.excluded() != settings.excludedFileTypes.toSet()

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
