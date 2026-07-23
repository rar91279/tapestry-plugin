package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.model.presentation.InjectedElement;

import javax.swing.tree.DefaultMutableTreeNode;

public class EmbeddedComponentNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 8480011580669274491L;

    private final transient InjectedElement _injectedComponent;
    private final String _label;

    public EmbeddedComponentNode(InjectedElement injectedComponent) {
        super(injectedComponent);

        _injectedComponent = injectedComponent;
        // Precompute the label (touches PSI) here — the node is built inside a read action, but
        // toString() is called on the EDT during rendering where PSI access is not allowed.
        _label = injectedComponent.getElementId();
    }

    public InjectedElement getInjectedComponent() {
        return _injectedComponent;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return _label;
    }
}
