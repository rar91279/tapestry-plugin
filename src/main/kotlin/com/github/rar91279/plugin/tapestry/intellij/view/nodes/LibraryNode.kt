package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDirectory
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary

class LibraryNode(library: TapestryLibrary, psiDirectory: PsiDirectory, module: Module) :
    PackageNode(library, psiDirectory, module) {
    init {
        init(psiDirectory, PresentationData(psiDirectory.name, psiDirectory.name, AllIcons.Nodes.PpLib, null))
    }
}
