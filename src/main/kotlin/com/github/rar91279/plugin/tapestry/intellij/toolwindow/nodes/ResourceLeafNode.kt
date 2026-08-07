package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.resource.IResource
import javax.swing.tree.DefaultMutableTreeNode

/** A navigable leaf wrapping a single resource file (template, message catalog, ...). */
class ResourceLeafNode(resource: IResource) : DefaultMutableTreeNode(resource) {

    private val label = resource.name

    override fun toString() = label
}
