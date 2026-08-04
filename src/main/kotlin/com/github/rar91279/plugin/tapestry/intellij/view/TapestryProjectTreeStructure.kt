package com.github.rar91279.plugin.tapestry.intellij.view

import com.intellij.ide.util.treeView.NodeDescriptor
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.RootNode
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure

/**
 * Defines the basic tree structure.
 */
class TapestryProjectTreeStructure(private val rootNode: RootNode) : SimpleTreeStructure() {

    private object EmptyDescriptor : SimpleNode() {
        override fun getChildren(): Array<SimpleNode> = arrayOf()
        override fun getEqualityObjects(): Array<Any> = arrayOf("EMPTY_DESCRIPTOR")
    }

    override fun getRootElement(): RootNode = rootNode

    override fun getParentElement(element: Any): Any? =
        try {
            (element as SimpleNode).parent
        } catch (e: ClassCastException) {
            null
        }

    override fun createDescriptor(element: Any, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<*> =
        element as? NodeDescriptor<*> ?: EmptyDescriptor
}
