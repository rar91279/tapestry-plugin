package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.InjectedElement
import javax.swing.tree.DefaultMutableTreeNode

sealed class InjectedElementNode(val injected: InjectedElement, private val label: String) :
    DefaultMutableTreeNode(injected) {

    override fun toString() = label
}
