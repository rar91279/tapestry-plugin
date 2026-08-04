package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile

class FileNode(file: PsiFile, module: Module) : TapestryNode(module) {
    init {
        init(file, PresentationData(file.name, file.name, file.fileType.icon, null))
    }
}
