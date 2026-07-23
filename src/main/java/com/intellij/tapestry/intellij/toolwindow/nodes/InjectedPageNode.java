package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.model.presentation.InjectedElement;

import javax.swing.tree.DefaultMutableTreeNode;

public class InjectedPageNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = -937413784681186436L;

    private final transient InjectedElement _injectedPage;
    private final String _label;

    public InjectedPageNode(InjectedElement injectedPage) {
        super(injectedPage);

        _injectedPage = injectedPage;
        // Precompute the label (touches PSI) here — toString() runs on the EDT during rendering.
        _label = injectedPage.getElementId();
    }

    public InjectedElement getInjectedPage() {
        return _injectedPage;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return _label;
    }
}
