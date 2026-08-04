package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClassOwner
import com.github.rar91279.plugin.tapestry.core.model.presentation.Page
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResource
import com.intellij.ui.treeStructure.SimpleNode
import icons.TapestryIcons

/**
 * Page node.
 */
class PageNode(page: PresentationLibraryElement, module: Module) : TapestryNode(module) {

    init {
        init(page, PresentationData(page.elementClass.name, page.name, TapestryIcons.Page, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val page = getValue() as Page
        val children = ArrayList<SimpleNode>()

        children.add(ClassNode((page.elementClass as IntellijJavaClassType).psiClass!!.containingFile as PsiClassOwner, module))

        for (template in page.template) children.add(FileNode((template as IntellijResource).psiFile, module))
        for (catalog in page.messageCatalog) children.add(FileNode((catalog as IntellijResource).psiFile, module))

        return children.toTypedArray()
    }
}
