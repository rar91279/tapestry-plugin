package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.resource.IResource;

import javax.swing.tree.DefaultMutableTreeNode;

/** A navigable leaf wrapping a single resource file (template, message catalog, ...). */
public class ResourceLeafNode extends DefaultMutableTreeNode {

    private final String _label;

    public ResourceLeafNode(IResource resource) {
        super(resource);
        // Precompute the label here (read action); toString() runs on the EDT during rendering.
        _label = resource.getName();
    }

    public String toString() {
        return _label;
    }
}
