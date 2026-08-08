package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.intellij.ui.treeStructure.SimpleNode

class ExternalLibraryNode(library: TapestryLibrary, module: Module) : TapestryNode(module) {

    init {
        init(library, PresentationData(library.id, library.id, AllIcons.Nodes.PpLib, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val children = ArrayList<SimpleNode>()
        val library = getValue() as TapestryLibrary

        if (library.pages.isNotEmpty()) {
            children.add(PagesNode(library,
                JavaPsiFacade.getInstance(myProject).findPackage(library.basePackage + "." + TapestryConstants.PAGES_PACKAGE)!!
                    .getDirectories(GlobalSearchScope.moduleWithLibrariesScope(module))[0], module))
        }

        if (library.components.isNotEmpty()) {
            children.add(ComponentsNode(library,
                JavaPsiFacade.getInstance(myProject).findPackage(library.basePackage + "." + TapestryConstants.COMPONENTS_PACKAGE)!!
                    .getDirectories(GlobalSearchScope.moduleWithLibrariesScope(module))[0], module))
        }

        if (library.mixins.isNotEmpty()) {
            children.add(MixinsNode(library,
                JavaPsiFacade.getInstance(myProject).findPackage(library.basePackage + "." + TapestryConstants.MIXINS_PACKAGE)!!
                    .getDirectories(GlobalSearchScope.moduleWithLibrariesScope(module))[0], module))
        }

        return children.toTypedArray()
    }
}
