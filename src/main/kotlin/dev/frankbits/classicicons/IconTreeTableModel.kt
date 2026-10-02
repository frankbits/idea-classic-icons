package dev.frankbits.classicicons

import com.intellij.ui.components.JBTreeTable
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import javax.swing.Icon
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.table.AbstractTableModel
import javax.swing.tree.DefaultMutableTreeNode

/**
 * A tree table model that displays icons in a hierarchical structure with columns for
 * checkbox (Classic), icon preview, path, file types, and extensions.
 */
class IconTreeTableModel(private val treeModel: FileTypeTreeModel) : TreeTableModel, AbstractTableModel() {
    
    private val columnNames = arrayOf("Classic", "Icon", "Path", "File Types", "Extensions")
    private val columnClasses = arrayOf(Boolean::class.java, Icon::class.java, String::class.java, String::class.java, String::class.java)
    
    // TreeModelListener to respond to tree model changes
    private val treeModelListener = object : TreeModelListener {
        override fun treeNodesChanged(e: TreeModelEvent) {
            fireTableDataChanged()
        }
        override fun treeNodesInserted(e: TreeModelEvent) {
            fireTableDataChanged()
        }
        override fun treeNodesRemoved(e: TreeModelEvent) {
            fireTableDataChanged()
        }
        override fun treeStructureChanged(e: TreeModelEvent) {
            fireTableDataChanged()
        }
    }
    
    init {
        treeModel.addTreeModelListener(treeModelListener)
    }
    
    override fun getColumnCount(): Int = columnNames.size
    
    override fun getColumnName(column: Int): String = columnNames[column]
    
    override fun getColumnClass(column: Int): Class<*> = columnClasses[column]
    
    override fun isCellEditable(row: Int, column: Int): Boolean {
        // Only the "Classic" column (column 0) is editable
        if (column != 0) return false
        
        val node = getNodeForRow(row) ?: return false
        
        return when (val userObject = node.userObject) {
            is FileTypeTreeModel.IconPathNode -> userObject.hasClassicEquivalent
            is FileTypeTreeModel.FileTypeNode -> true
            else -> false
        }
    }
    
    override fun getRowCount(): Int = countSubtreeSize(treeModel.root)
    
    override fun getValueAt(row: Int, column: Int): Any? {
        val node = getNodeForRow(row) ?: return null
        
        return when (val userObject = node.userObject) {
            is FileTypeTreeModel.CategoryNode -> {
                when (column) {
                    0 -> null // No checkbox for categories
                    1 -> null // No icon for categories
                    2 -> userObject.category
                    3 -> ""
                    4 -> ""
                    else -> null
                }
            }
            is FileTypeTreeModel.IconPathNode -> {
                when (column) {
                    0 -> userObject.classic
                    1 -> userObject.icon
                    2 -> userObject.path
                    3 -> userObject.fileTypes.joinToString(", ")
                    4 -> userObject.extensions.joinToString(", ")
                    else -> null
                }
            }
            is FileTypeTreeModel.FileTypeNode -> {
                when (column) {
                    0 -> userObject.classic
                    1 -> null // No icon for individual file types
                    2 -> userObject.path
                    3 -> userObject.fileType
                    4 -> userObject.extension
                    else -> null
                }
            }
            else -> null
        }
    }
    
    override fun setValueAt(value: Any?, row: Int, column: Int) {
        if (column != 0) return // Only column 0 (Classic) is editable
        
        val node = getNodeForRow(row) ?: return
        
        when (val userObject = node.userObject) {
            is FileTypeTreeModel.IconPathNode -> {
                if (userObject.hasClassicEquivalent) {
                    val newValue = value as? Boolean ?: return
                    userObject.classic = newValue
                    treeModel.setAllClassic(userObject, newValue)
                    fireTableCellUpdated(row, column)
                }
            }
            is FileTypeTreeModel.FileTypeNode -> {
                val newValue = value as? Boolean ?: return
                userObject.classic = newValue
                val parent = node.parent as? DefaultMutableTreeNode ?: return
                val parentUserObject = parent.userObject as? FileTypeTreeModel.IconPathNode ?: return
                treeModel.updateFromChildren(parentUserObject)
                fireTableCellUpdated(row, column)
            }
        }
    }
    
    override fun getChildCount(parent: Any?): Int {
        if (parent == null) {
            return treeModel.root.childCount
        }
        return (parent as? DefaultMutableTreeNode)?.childCount ?: 0
    }
    
    override fun getChild(parent: Any?, index: Int): Any? {
        if (parent == null) {
            return treeModel.root.getChildAt(index)
        }
        return (parent as? DefaultMutableTreeNode)?.getChildAt(index)
    }
    
    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        if (parent == null || child == null) return -1
        return (parent as? DefaultMutableTreeNode)?.getIndex(child as DefaultMutableTreeNode) ?: -1
    }
    
    override fun getRoot(): Any = treeModel.root
    
    override fun isLeaf(node: Any?): Boolean {
        return (node as? DefaultMutableTreeNode)?.childCount == 0
    }
    
    override fun addTreeModelListener(l: TreeModelListener) {
        treeModel.addTreeModelListener(l)
    }
    
    override fun removeTreeModelListener(l: TreeModelListener) {
        treeModel.removeTreeModelListener(l)
    }
    
    private fun getNodeForRow(row: Int): DefaultMutableTreeNode? {
        val root = treeModel.root
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
        var count = 1 // Count this node
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as DefaultMutableTreeNode
            count += countSubtreeSize(child)
        }
        return count
    }
    
    override fun getTree(): JBTreeTable? = null
    
    override fun setTree(tree: JBTreeTable?) {}
}
