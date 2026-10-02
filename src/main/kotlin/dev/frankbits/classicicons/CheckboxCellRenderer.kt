package dev.frankbits.classicicons

import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JCheckBox
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom renderer for the Classic checkbox column in the tree table.
 * Disables checkboxes for icons without classic equivalent.
 */
class CheckboxCellRenderer(private val treeModel: FileTypeTreeModel) : TableCellRenderer {
    private val checkbox = JCheckBox()
    
    init {
        checkbox.isOpaque = false
        checkbox.horizontalAlignment = JCheckBox.CENTER
        checkbox.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                e.consume()
            }
        })
    }
    
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        // Get the node for this row
        val node = getNodeForRow(table, row)
        
        // Configure appearance
        checkbox.isSelected = value as? Boolean ?: false
        
        if (node != null) {
            when (val userObject = node.userObject) {
                is FileTypeTreeModel.CategoryNode -> {
                    // Categories don't have checkboxes
                    checkbox.isEnabled = false
                    checkbox.isSelected = false
                }
                is FileTypeTreeModel.IconPathNode -> {
                    // Enable checkbox only if icon has classic equivalent
                    checkbox.isEnabled = userObject.hasClassicEquivalent
                }
                is FileTypeTreeModel.FileTypeNode -> {
                    // File types always have checkboxes enabled
                    checkbox.isEnabled = true
                }
                else -> {
                    checkbox.isEnabled = false
                }
            }
        } else {
            checkbox.isEnabled = false
        }
        
        // Set background for selection
        if (isSelected) {
            checkbox.background = table.selectionBackground
        } else {
            checkbox.background = table.background
        }
        
        return checkbox
    }
    
    private fun getNodeForRow(table: JTable, row: Int): DefaultMutableTreeNode? {
        val model = table.model as? IconTreeTableModel ?: return null
        val root = model.getRoot() as? DefaultMutableTreeNode ?: return null
        
        var currentRow = row
        var currentNode: DefaultMutableTreeNode = root
        
        // Navigate to the node at the given row
        while (currentRow >= 0 && currentNode.childCount > 0) {
            for (i in 0 until currentNode.childCount) {
                val child = currentNode.getChildAt(i) as DefaultMutableTreeNode
                val subtreeSize = countSubtreeSize(child)
                
                if (currentRow < subtreeSize) {
                    currentNode = child
                    currentRow--
                    break
                } else {
                    currentRow -= subtreeSize
                }
            }
        }
        
        return if (currentRow == 0) currentNode else null
    }
    
    private fun countSubtreeSize(node: DefaultMutableTreeNode): Int {
        var count = 1
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as DefaultMutableTreeNode
            count += countSubtreeSize(child)
        }
        return count
    }
}
