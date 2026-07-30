package com.intellij.tapestry.intellij.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.ui.treeStructure.SimpleNode

class LibrariesNode(module: Module) : TapestryNode(module) {

    init {
        init("Libraries", PresentationData("Libraries", "Libraries", AllIcons.Nodes.PpLib, null))
    }

    override fun getChildren(): Array<SimpleNode> =
        TapestryModuleSupportLoader.getTapestryProject(module)!!.libraries
            .filter { it.id != TapestryProject.APPLICATION_LIBRARY_ID }
            .map { ExternalLibraryNode(it, module) as SimpleNode }
            .toTypedArray()
}
