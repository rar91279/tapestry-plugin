package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.intellij.psi.PsiFile
import javax.swing.tree.DefaultMutableTreeNode

/** A navigable leaf wrapping a single resource file (template, message catalog, ...). */
class ResourceLeafNode(resource: PsiFile) : DefaultMutableTreeNode(resource) {

    private val label = resource.name

    override fun toString() = label
}
