package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import javax.swing.tree.DefaultMutableTreeNode

class MessageCatalogNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        for (catalog in element.messageCatalog) add(ResourceLeafNode(catalog))
    }

    override fun toString() = "Message Catalog"
}
