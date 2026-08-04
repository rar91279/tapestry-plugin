package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.PlatformIcons

abstract class AbstractModuleNode(module: Module) : TapestryNode(module) {

    init {
        init(module, PresentationData(module.name, module.name, PlatformIcons.WEB_ICON, null))
    }

    abstract override fun getChildren(): Array<SimpleNode>
}
