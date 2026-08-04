package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClassOwner
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResource
import com.intellij.ui.treeStructure.SimpleNode
import icons.TapestryIcons

/**
 * Component node.
 */
class ComponentNode(component: PresentationLibraryElement, module: Module) : TapestryNode(module) {

    init {
        init(component, PresentationData(component.elementClass.name, component.elementClass.name, TapestryIcons.Component, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val component = getValue() as TapestryComponent
        val children = ArrayList<SimpleNode>()

        children.add(ClassNode((component.elementClass as IntellijJavaClassType).psiClass!!.containingFile as PsiClassOwner, module))

        for (template in component.template) children.add(FileNode((template as IntellijResource).psiFile, module))
        for (catalog in component.messageCatalog) children.add(FileNode((catalog as IntellijResource).psiFile, module))

        return children.toTypedArray()
    }
}
