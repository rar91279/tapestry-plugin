package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import javax.swing.tree.DefaultMutableTreeNode

/** A navigable leaf for an element that embeds or injects the shown element. */
class UsageNode(user: PresentationLibraryElement, val kind: TapestryProject.UsageKind) :
    DefaultMutableTreeNode(user) {

    private val label = user.name.orEmpty()

    override fun toString() = label
}
