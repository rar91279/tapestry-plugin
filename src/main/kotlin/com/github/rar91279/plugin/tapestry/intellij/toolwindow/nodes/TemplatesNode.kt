package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import javax.swing.tree.DefaultMutableTreeNode

class TemplatesNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        for (template in element.template) add(ResourceLeafNode(template))
    }

    override fun toString() = "Templates"
}
