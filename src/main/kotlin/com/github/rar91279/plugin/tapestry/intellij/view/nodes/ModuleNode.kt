package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiManager
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.view.TapestryProjectViewPane
import com.intellij.ui.treeStructure.SimpleNode
import java.util.TreeSet

/**
 * Tapestry module node.
 */
class ModuleNode(module: Module) : AbstractModuleNode(module) {

    override fun getChildren(): Array<SimpleNode> {
        val children = TreeSet<TapestryNode>(PackageNodesComparator)
        val moduleFileIndex = ModuleRootManager.getInstance(getValue() as Module).fileIndex

        moduleFileIndex.iterateContent { virtualFile ->
            if (virtualFile.isDirectory && moduleFileIndex.isInSourceContent(virtualFile)) {
                val psiDirectory = PsiManager.getInstance(myProject).findDirectory(virtualFile)
                val aPackage = IdeaUtils.getPackage(psiDirectory)

                if (TapestryProjectViewPane.getInstance(myProject).isFromBasePackage) {
                    val applicationLibrary = TapestryModuleSupportLoader.getTapestryProject(module)!!.applicationLibrary!!
                    if (aPackage?.name != null && aPackage.qualifiedName == applicationLibrary.basePackage) {
                        children.add(LibraryNode(applicationLibrary, psiDirectory!!, module))
                    }
                } else if (aPackage?.name != null && (aPackage.parentPackage == null || aPackage.parentPackage!!.name == null)) {
                    children.add(PackageNode(PsiManager.getInstance(myProject).findDirectory(virtualFile)!!, getValue() as Module))
                }
            }
            true
        }

        if (TapestryProjectViewPane.getInstance(myProject).isShowLibraries) {
            children.add(LibrariesNode(module))
        }

        return children.map { it as SimpleNode }.toTypedArray()
    }
}
