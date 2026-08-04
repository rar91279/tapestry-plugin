package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClassOwner
import com.intellij.util.PlatformIcons

class ClassNode(psiClassOwner: PsiClassOwner, module: Module) : TapestryNode(module) {
    init {
        init(psiClassOwner, PresentationData(psiClassOwner.name, psiClassOwner.name, PlatformIcons.CLASS_ICON, null))
    }
}
