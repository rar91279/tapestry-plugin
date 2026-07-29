package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.TapestryProject;
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;

import javax.swing.tree.DefaultMutableTreeNode;

public class UsagesNode extends DefaultMutableTreeNode {

    public UsagesNode(Object userObject) {
        super(userObject);

        PresentationLibraryElement element = (PresentationLibraryElement) userObject;
        for (TapestryProject.Usage usage : element.getProject().findUsages(element)) {
            add(new UsageNode(usage.user(), usage.kind()));
        }
    }

    public String toString() {
        return "Used By";
    }
}
