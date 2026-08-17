package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDirectory
import com.intellij.ui.treeStructure.SimpleNode
import icons.TapestryIcons

/** The mixins of a module. */
class MixinsNode(
    module: Module,
    directories: List<PsiDirectory>,
    elements: Collection<PresentationLibraryElement>,
    showElementFiles: Boolean = true
) : ElementCategoryNode("Mixins", TapestryIcons.Mixins, module, directories, elements, showElementFiles) {

    override fun elementNode(element: PresentationLibraryElement, module: Module): SimpleNode =
        MixinNode(element, module, showElementFiles)
}
