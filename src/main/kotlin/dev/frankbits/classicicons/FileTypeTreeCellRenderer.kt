package dev.frankbits.classicicons

import java.awt.Component
import java.awt.Font
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer
import java.awt.FlowLayout

/**
 * Custom TreeCellRenderer für die Icon-Baumansicht.
 * Zeigt Checkboxen für IconPathNodes und FileTypeNodes an.
 * Kategorien werden ohne Checkboxen angezeigt.
 */
class FileTypeTreeCellRenderer(private val treeModel: FileTypeTreeModel) : TreeCellRenderer {
    private val defaultRenderer = DefaultTreeCellRenderer()
    private val checkbox = JCheckBox()
    private val iconLabel = JLabel()
    private val textLabel = JLabel()
    private val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
    
    init {
        checkbox.isOpaque = false
        iconLabel.isOpaque = false
        textLabel.isOpaque = false
        panel.isOpaque = false
        
        panel.add(checkbox)
        panel.add(iconLabel)
        panel.add(textLabel)
    }

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val node = value as? DefaultMutableTreeNode ?: return defaultRenderer.getTreeCellRendererComponent(
            tree, value, selected, expanded, leaf, row, hasFocus
        )
        
        // Hintergrund und Auswahl
        if (selected) {
            panel.background = defaultRenderer.backgroundSelectionColor
            iconLabel.background = defaultRenderer.backgroundSelectionColor
            textLabel.background = defaultRenderer.backgroundSelectionColor
        } else {
            panel.background = defaultRenderer.backgroundNonSelectionColor
            iconLabel.background = defaultRenderer.backgroundNonSelectionColor
            textLabel.background = defaultRenderer.backgroundNonSelectionColor
        }
        
        // Text und Icon setzen
        when (val userObject = node.userObject) {
            is FileTypeTreeModel.CategoryNode -> {
                checkbox.isVisible = false
                iconLabel.icon = null
                iconLabel.text = ""
                textLabel.text = userObject.category
                panel.toolTipText = treeModel.getIconTooltip(node)
                iconLabel.border = null
                
                // Fett für Hauptkategorien
                textLabel.font = textLabel.font.deriveFont(Font.BOLD)
            }
            is FileTypeTreeModel.IconPathNode -> {
                // Checkbox ist immer sichtbar, aber nur aktiviert wenn es ein klassisches Äquivalent gibt
                // ODER wenn ein Custom Icon existiert
                val hasCustomIcon = treeModel.hasCustomIcon(node)
                val canBeReplaced = userObject.hasClassicEquivalent || hasCustomIcon
                
                checkbox.isVisible = true
                checkbox.isSelected = userObject.classic
                checkbox.isEnabled = canBeReplaced
                
                // Icon anzeigen
                if (userObject.icon != null) {
                    iconLabel.icon = userObject.icon
                    iconLabel.text = ""
                } else {
                    iconLabel.icon = null
                    iconLabel.text = ""
                }
                
                textLabel.text = userObject.path
                panel.toolTipText = treeModel.getIconTooltip(node)
                
                // Textfarbe: normal für ersetzbare Icons
                textLabel.foreground = defaultRenderer.textNonSelectionColor
                iconLabel.foreground = defaultRenderer.textNonSelectionColor
                
                // Farbiger Indikator für Custom Icons
                iconLabel.border = if (hasCustomIcon) {
                    javax.swing.border.CompoundBorder(
                        javax.swing.border.MatteBorder(0, 1, 0, 0, java.awt.Color.ORANGE),
                        javax.swing.border.EmptyBorder(0, 0, 0, 0)
                    )
                } else {
                    null
                }
            }
            is FileTypeTreeModel.FileTypeNode -> {
                checkbox.isVisible = true
                checkbox.isSelected = userObject.classic
                checkbox.isEnabled = true
                iconLabel.icon = null
                iconLabel.text = ""
                textLabel.text = "${userObject.fileType}${if (userObject.extension.isNotEmpty()) " (${userObject.extension})" else ""}"
                panel.toolTipText = "Icon path: ${userObject.path}"
                iconLabel.border = null
                textLabel.foreground = defaultRenderer.textNonSelectionColor
            }
            else -> {
                checkbox.isVisible = false
                iconLabel.icon = defaultRenderer.getTreeCellRendererComponent(
                    tree, value, selected, expanded, leaf, row, hasFocus
                ).let { it as? JLabel }?.icon
                iconLabel.text = ""
                textLabel.text = node.toString()
                panel.toolTipText = null
                iconLabel.border = null
            }
        }
        
        return panel
    }
}
