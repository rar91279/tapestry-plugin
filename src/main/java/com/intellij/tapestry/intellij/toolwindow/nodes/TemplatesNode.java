package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;
import com.intellij.tapestry.core.resource.IResource;

import javax.swing.tree.DefaultMutableTreeNode;

public class TemplatesNode extends DefaultMutableTreeNode {

    public TemplatesNode(Object userObject) {
        super(userObject);

        for (IResource template : ((PresentationLibraryElement) userObject).getTemplate()) {
            add(new ResourceLeafNode(template));
        }
    }

    public String toString() {
        return "Templates";
    }
}
