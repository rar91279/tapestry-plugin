package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import javax.swing.tree.DefaultMutableTreeNode

class EmbeddedComponentsNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        if (element.hasClassFile) {
            for (template in element.template) add(EmbeddedTemplateNode(template, element))
            for (embedded in element.embeddedComponents) {
                if (embedded.template == "class") embedded.element?.let { add(EmbeddedComponentNode(it)) }
            }
        }
    }

    override fun toString() = "Embedded Components"
}
