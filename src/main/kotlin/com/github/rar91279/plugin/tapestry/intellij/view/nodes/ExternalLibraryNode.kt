package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.intellij.ui.treeStructure.SimpleNode

/** A Tapestry library the module depends on, listing the elements it provides. */
class ExternalLibraryNode(library: TapestryLibrary, module: Module) : TapestryNode(module) {

    init {
        init(library, PresentationData(library.id, library.id, AllIcons.Nodes.PpLib, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val library = getValue() as TapestryLibrary
        val children = ArrayList<SimpleNode>()

        if (library.pages.isNotEmpty()) {
            children.add(
                PagesNode(module, directoriesOf(library, TapestryConstants.PAGES_PACKAGE), library.pages.values)
            )
        }

        if (library.components.isNotEmpty()) {
            children.add(
                ComponentsNode(
                    module, directoriesOf(library, TapestryConstants.COMPONENTS_PACKAGE), library.components.values
                )
            )
        }

        if (library.mixins.isNotEmpty()) {
            children.add(
                MixinsNode(module, directoriesOf(library, TapestryConstants.MIXINS_PACKAGE), library.mixins.values)
            )
        }

        return children.toTypedArray()
    }

    /** A library's classes come from a jar, so its packages may resolve to no directory at all. */
    private fun directoriesOf(library: TapestryLibrary, subPackage: String): List<PsiDirectory> =
        JavaPsiFacade.getInstance(myProject).findPackage("${library.basePackage}.$subPackage")
            ?.getDirectories(GlobalSearchScope.moduleWithLibrariesScope(module))
            ?.asList()
            .orEmpty()
}
