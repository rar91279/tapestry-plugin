package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.psi.PsiFile
import javax.swing.tree.DefaultMutableTreeNode

class EmbeddedTemplateNode(resource: PsiFile, element: PresentationLibraryElement) :
    DefaultMutableTreeNode(resource) {

    private val label = resource.virtualFile?.nameWithoutExtension ?: resource.name

    init {
        if (element.hasClassFile) {
            for (embedded in element.embeddedComponentsTemplate) {
                if (embedded.template == resource.name) embedded.element?.let { add(EmbeddedComponentNode(it)) }
            }
        }
    }

    override fun toString() = label
}
