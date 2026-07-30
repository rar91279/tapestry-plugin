package com.intellij.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClassOwner
import com.intellij.tapestry.core.model.presentation.Mixin
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType
import com.intellij.ui.treeStructure.SimpleNode
import icons.TapestryIcons

/**
 * A Mixin node.
 */
class MixinNode(mixin: PresentationLibraryElement, module: Module) : TapestryNode(module) {

    init {
        init(mixin, PresentationData(mixin.name, mixin.name, TapestryIcons.Mixin, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val mixin = getValue() as Mixin
        return arrayOf(ClassNode((mixin.elementClass as IntellijJavaClassType).psiClass!!.containingFile as PsiClassOwner, module))
    }
}
