package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.TapestryProject;
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;

import javax.swing.tree.DefaultMutableTreeNode;

/** A navigable leaf for an element that embeds or injects the shown element. */
public class UsageNode extends DefaultMutableTreeNode {

    private final String _label;
    private final TapestryProject.UsageKind _kind;

    public UsageNode(PresentationLibraryElement user, TapestryProject.UsageKind kind) {
        super(user);
        // Precompute the label here (read action); toString() runs on the EDT during rendering.
        _label = user.getName();
        _kind = kind;
    }

    /** How the referencing element uses the shown element (template vs. Java injection). */
    public TapestryProject.UsageKind getKind() {
        return _kind;
    }

    public String toString() {
        return _label;
    }
}
