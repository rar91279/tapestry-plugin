package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;

import javax.swing.tree.DefaultMutableTreeNode;

public class UsagesNode extends DefaultMutableTreeNode {

    public UsagesNode(Object userObject) {
        super(userObject);

        PresentationLibraryElement element = (PresentationLibraryElement) userObject;
        for (PresentationLibraryElement user : element.getProject().findUsages(element)) {
            add(new UsageNode(user));
        }
    }

    public String toString() {
        return "Used By";
    }
}
