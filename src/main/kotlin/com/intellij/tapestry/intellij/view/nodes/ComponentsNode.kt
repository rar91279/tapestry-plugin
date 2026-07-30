package com.intellij.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDirectory
import com.intellij.tapestry.core.model.TapestryLibrary
import icons.TapestryIcons

class ComponentsNode : PackageNode {
    constructor(library: TapestryLibrary, psiDirectory: PsiDirectory, module: Module) : super(library, psiDirectory, module) {
        init(psiDirectory, PresentationData(psiDirectory.name, psiDirectory.name, TapestryIcons.Components, null))
    }

    constructor(psiDirectory: PsiDirectory, module: Module) : super(psiDirectory, module) {
        init(psiDirectory, PresentationData(psiDirectory.name, psiDirectory.name, TapestryIcons.Components, null))
    }
}
