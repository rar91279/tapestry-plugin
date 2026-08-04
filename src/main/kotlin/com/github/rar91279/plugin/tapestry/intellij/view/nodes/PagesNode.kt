package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDirectory
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import icons.TapestryIcons

class PagesNode : PackageNode {
    constructor(library: TapestryLibrary, psiDirectory: PsiDirectory, module: Module) : super(library, psiDirectory, module) {
        init(psiDirectory, PresentationData(psiDirectory.name, psiDirectory.name, TapestryIcons.Pages, null))
    }

    constructor(psiDirectory: PsiDirectory, module: Module) : super(psiDirectory, module) {
        init(psiDirectory, PresentationData(psiDirectory.name, psiDirectory.name, TapestryIcons.Pages, null))
    }
}
