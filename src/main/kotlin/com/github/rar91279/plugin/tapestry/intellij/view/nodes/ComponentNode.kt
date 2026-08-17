package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClassOwner
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.intellij.ui.treeStructure.SimpleNode
import icons.TapestryIcons

/**
 * Component node.
 */
class ComponentNode(
    component: PresentationLibraryElement,
    module: Module,
    private val showFiles: Boolean = true
) : TapestryNode(module) {

    init {
        init(component, PresentationData(component.elementClass?.name, component.elementClass?.name, TapestryIcons.Component, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        if (!showFiles) return NO_CHILDREN

        val component = getValue() as TapestryComponent
        val children = ArrayList<SimpleNode>()

        (component.elementClass?.containingFile as? PsiClassOwner)?.let { children.add(ClassNode(it, module)) }

        for (template in component.template) children.add(FileNode(template, module))
        for (catalog in component.messageCatalog) children.add(FileNode(catalog, module))
        for (asset in component.assets) children.add(FileNode(asset, module))

        return children.toTypedArray()
    }
}
