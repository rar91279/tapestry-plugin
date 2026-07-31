package com.intellij.tapestry.intellij.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.tapestry.core.resource.IResource
import com.intellij.tapestry.intellij.toolwindow.nodes.*
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import icons.TapestryIcons
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom tree cell renderer for the Tapestry dependencies tool window.
 *
 * Renders tree nodes with appropriate icons and text based on the node type,
 * distinguishing between pages, components, mixins, templates, message catalogs,
 * and various dependency relationships in a Tapestry application.
 */
class DependenciesTreeCellRenderer : ColoredTreeCellRenderer() {
    /**
     * Customizes the rendering of a tree cell by setting its text and icon.
     *
     * @param tree the JTree being rendered
     * @param value the value of the current tree cell
     * @param selected whether the cell is selected
     * @param expanded whether the cell is expanded
     * @param leaf whether the cell is a leaf node
     * @param row the row index of the cell
     * @param hasFocus whether the cell has focus
     */
    override fun customizeCellRenderer(
        tree: JTree, value: Any?, selected: Boolean, expanded: Boolean,
        leaf: Boolean, row: Int, hasFocus: Boolean
    ) {
        append(
            tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus),
            SimpleTextAttributes.REGULAR_ATTRIBUTES
        )
        setIcon(iconFor(value))
    }

    /**
     * Determines the appropriate icon for a tree node based on its type.
     *
     * Returns different icons for:
     * - Tapestry elements (pages, components, mixins)
     * - Usage references (template-based or Java injection-based)
     * - Resource nodes (templates, message catalogs, generic resources)
     * - Container nodes (embedded components, injected pages, etc.)
     *
     * @param value the tree node object
     * @return the icon to display for the node, or null if no specific icon applies
     */
    private fun iconFor(value: Any?): Icon? {
        when (value) {
            is DependenciesRootNode -> {
                return when (((value as DefaultMutableTreeNode).getUserObject() as PresentationLibraryElement).getElementType()) {
                    PresentationLibraryElement.ElementType.PAGE -> AllIcons.Nodes.Controller
                    PresentationLibraryElement.ElementType.COMPONENT -> AllIcons.Nodes.Class
                    PresentationLibraryElement.ElementType.MIXIN -> AllIcons.Nodes.Aspect
                    else -> null
                }
            }
            // "Used By" leaves differentiate by how they reference the element: template vs. Java injection.
            is UsageNode -> {
                return when (value.kind) {
                    TapestryProject.UsageKind.TEMPLATE -> TapestryIcons.Tapestry_logo_small
                    TapestryProject.UsageKind.INJECTED -> AllIcons.Nodes.Class
                }
            }

            is EmbeddedComponentsNode -> return AllIcons.Nodes.Package
            is InjectedPagesNode -> return AllIcons.Nodes.WebFolder
            is EmbeddedComponentNode -> return AllIcons.Nodes.Class
            is InjectedPageNode -> return AllIcons.Nodes.Controller
            is TemplatesNode -> return AllIcons.FileTypes.Html
            is MessageCatalogNode -> return AllIcons.FileTypes.Properties
            // Leaf resource files: differentiate by extension so templates and message catalogs read
            // apart from generic resources at a glance.
            is ResourceLeafNode -> {
                val resource = value.getUserObject() as IResource
                return when (resource.getExtension()) {
                    "tml" -> TapestryIcons.Tapestry_logo_small
                    "properties" -> AllIcons.Toolwindows.ToolWindowMessages
                    else -> AllIcons.FileTypes.Any_type
                }
            }

            is UsagesNode -> return AllIcons.Hierarchy.Subtypes
            else -> return null
        }
    }
}
