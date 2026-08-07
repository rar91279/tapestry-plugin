package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import javax.swing.tree.DefaultMutableTreeNode

class UsagesNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        for (usage in element.project.findUsages(element)) add(UsageNode(usage.user, usage.kind))
    }

    override fun toString() = "Used By"
}
