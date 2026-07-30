package com.intellij.tapestry.intellij.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;
import com.intellij.tapestry.core.resource.IResource;
import com.intellij.tapestry.intellij.toolwindow.nodes.*;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import icons.TapestryIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class DependenciesTreeCellRenderer extends ColoredTreeCellRenderer {

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded,
                                      boolean leaf, int row, boolean hasFocus) {
        append(tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus),
               SimpleTextAttributes.REGULAR_ATTRIBUTES);
        setIcon(iconFor(value));
    }

    private static Icon iconFor(Object value) {
        if (value instanceof DependenciesRootNode) {
            return switch (((PresentationLibraryElement) ((DefaultMutableTreeNode) value).getUserObject()).getElementType()) {
                case PAGE -> AllIcons.Nodes.Controller;
                case COMPONENT -> AllIcons.Nodes.Class;
                case MIXIN -> AllIcons.Nodes.Aspect;
                default -> null;
            };
        }
        // "Used By" leaves differentiate by how they reference the element: template vs. Java injection.
        if (value instanceof UsageNode) {
            return switch (((UsageNode) value).getKind()) {
                case TEMPLATE -> TapestryIcons.Tapestry_logo_small;
                case INJECTED -> AllIcons.Nodes.Class;
            };
        }
        if (value instanceof EmbeddedComponentsNode) return AllIcons.Nodes.Package;
        if (value instanceof InjectedPagesNode) return AllIcons.Nodes.WebFolder;
        if (value instanceof EmbeddedComponentNode) return AllIcons.Nodes.Class;
        if (value instanceof InjectedPageNode) return AllIcons.Nodes.Controller;
        if (value instanceof TemplatesNode) return AllIcons.FileTypes.Html;
        if (value instanceof MessageCatalogNode) return AllIcons.FileTypes.Properties;
        // Leaf resource files: differentiate by extension so templates and message catalogs read
        // apart from generic resources at a glance.
        if (value instanceof ResourceLeafNode leaf) {
            IResource resource = (IResource) leaf.getUserObject();
            return switch (resource.getExtension()) {
                case "tml" -> TapestryIcons.Tapestry_logo_small;
                case "properties" -> AllIcons.Toolwindows.ToolWindowMessages;
                case null, default -> AllIcons.FileTypes.Any_type;
            };
        }
        if (value instanceof UsagesNode) return AllIcons.Hierarchy.Subtypes;
        return null;
    }
}
