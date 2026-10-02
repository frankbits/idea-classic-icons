package dev.frankbits.classicicons

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom renderer for the Icon column in the tree table.
 * Displays the icon with an orange border if a custom icon exists.
 */
class IconCellRenderer(private val treeModel: FileTypeTreeModel) : DefaultTableCellRenderer() {
    private val label = JLabel()
    
    init {
        label.isOpaque = true
        label.horizontalAlignment = JLabel.CENTER
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
        
        // Configure appearance based on selection
        if (isSelected) {
            label.background = table.selectionBackground
            label.foreground = table.selectionForeground
        } else {
            label.background = table.background
            label.foreground = table.foreground
        }
        
        // Set icon
        label.icon = value as? javax.swing.Icon
        
        // Check if we have a custom icon for this path
        if (node != null) {
            val hasCustomIcon = treeModel.hasCustomIcon(node)
            label.border = if (hasCustomIcon) {
                javax.swing.border.CompoundBorder(
                    javax.swing.border.MatteBorder(1, 1, 1, 1, java.awt.Color.ORANGE),
                    javax.swing.border.EmptyBorder(0, 2, 0, 2)
                )
            } else {
                javax.swing.border.EmptyBorder(2, 2, 2, 2)
            }
        } else {
            label.border = javax.swing.border.EmptyBorder(2, 2, 2, 2)
        }
        
        label.text = ""
        
        return label
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
