package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.resource.IResource
import javax.swing.tree.DefaultMutableTreeNode

class EmbeddedTemplateNode(resource: IResource, element: PresentationLibraryElement) :
    DefaultMutableTreeNode(resource) {

    private val label = resource.name.split("." + resource.extension)[0]

    init {
        if (element.hasClassFile) {
            for (embedded in element.embeddedComponentsTemplate) {
                if (embedded.template == resource.name) embedded.element?.let { add(EmbeddedComponentNode(it)) }
            }
        }
    }

    override fun toString() = label
}
