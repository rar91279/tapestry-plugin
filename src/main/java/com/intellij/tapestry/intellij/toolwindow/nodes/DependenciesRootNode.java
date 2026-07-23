package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;

import javax.swing.tree.DefaultMutableTreeNode;

public class DependenciesRootNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 7908744831198159929L;

    private final String _label;

    public DependenciesRootNode(Object userObject) {
        super(userObject);

        insert(new EmbeddedComponentsNode(userObject), 0);
        insert(new InjectedPagesNode(userObject), 0);

        // Precompute the label here (read action); toString() runs on the EDT during rendering.
        _label = ((PresentationLibraryElement) userObject).getName();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return _label;
    }
}
