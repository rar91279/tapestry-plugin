package com.github.rar91279.plugin.tapestry.intellij.view

import com.intellij.ide.IdeView
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class TapestryIdeView(private val viewPane: TapestryProjectViewPane) : IdeView {

    override fun getDirectories(): Array<PsiDirectory> {
        val directories = ArrayList<PsiDirectory>()
        val module = viewPane.getSelectedModule() ?: return PsiDirectory.EMPTY_ARRAY
        val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex

        moduleFileIndex.iterateContent { virtualFile ->
            if (virtualFile.isDirectory && moduleFileIndex.isInSourceContent(virtualFile)) {
                PsiManager.getInstance(viewPane.project).findDirectory(virtualFile)?.let { directories.add(it) }
            }
            true
        }
        return directories.toTypedArray()
    }

    override fun getOrChooseDirectory(): PsiDirectory? {
        val element = viewPane.getSelectedNodeElement()
        return when (element) {
            is PsiDirectory -> element
            is PsiFile -> element.containingDirectory
            else -> null
        }
    }
}
