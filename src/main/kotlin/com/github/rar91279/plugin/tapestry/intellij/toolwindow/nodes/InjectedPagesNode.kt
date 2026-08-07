package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import javax.swing.tree.DefaultMutableTreeNode

class InjectedPagesNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        if (element.hasClassFile) {
            for (injected in element.injectedPages) add(InjectedPageNode(injected))
        }
    }

    override fun toString() = "Injected Pages"
}
