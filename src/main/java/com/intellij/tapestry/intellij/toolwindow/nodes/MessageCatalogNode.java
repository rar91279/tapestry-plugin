package com.intellij.tapestry.intellij.toolwindow.nodes;

import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;
import com.intellij.tapestry.core.resource.IResource;

import javax.swing.tree.DefaultMutableTreeNode;

public class MessageCatalogNode extends DefaultMutableTreeNode {

    public MessageCatalogNode(Object userObject) {
        super(userObject);

        for (IResource catalog : ((PresentationLibraryElement) userObject).getMessageCatalog()) {
            add(new ResourceLeafNode(catalog));
        }
    }

    public String toString() {
        return "Message Catalog";
    }
}
