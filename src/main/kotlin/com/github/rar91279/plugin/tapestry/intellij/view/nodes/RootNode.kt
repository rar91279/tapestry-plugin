package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.openapi.project.Project
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.intellij.ui.treeStructure.SimpleNode

/**
 * Tapestry view root node.
 */
class RootNode(project: Project) : SimpleNode(project) {

    // Used only for getEqualityObjects() (node-identity diffing across tree refreshes). The root has
    // no backing domain object; getElement() is intentionally left at the SimpleNode default (`this`)
    // — see TapestryNode.getValue() for why overriding it here would break the async tree model.
    private val id = "ROOT"

    override fun getChildren(): Array<SimpleNode> =
        TapestryUtils.getAllTapestryModules(myProject).map { ModuleNode(it) as SimpleNode }.toTypedArray()

    override fun getEqualityObjects(): Array<Any> = arrayOf(id)
}
