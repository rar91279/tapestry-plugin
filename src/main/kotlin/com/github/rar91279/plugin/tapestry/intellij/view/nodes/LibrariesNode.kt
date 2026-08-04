package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
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
