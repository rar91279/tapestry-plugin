package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;

import javax.swing.tree.DefaultMutableTreeNode;

/** A navigable leaf for an element that embeds or injects the shown element. */
public class UsageNode extends DefaultMutableTreeNode {

    private final String _label;

    public UsageNode(PresentationLibraryElement user) {
        super(user);
        // Precompute the label here (read action); toString() runs on the EDT during rendering.
        _label = user.getName();
    }

    public String toString() {
        return _label;
    }
}
