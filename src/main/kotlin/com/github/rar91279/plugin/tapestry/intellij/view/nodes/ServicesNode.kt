package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.core.model.ioc.ModuleBuilder
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryModuleClasses
import com.intellij.ide.projectView.PresentationData
import com.intellij.java.ultimate.icons.JavaUltimateIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.ui.treeStructure.SimpleNode

/**
 * The IoC services the module itself declares.
 *
 * Only the module's own IoC module classes are read: the ones a manifest or the framework brings along declare
 * hundreds of services that belong to a library, not to this application. Discovery is
 * [TapestryModuleClasses], the same list the injected-bean gutter marker and JavaScript stack resolution use.
 */
class ServicesNode(module: Module) : TapestryNode(module) {

    init {
        init(TITLE, PresentationData(TITLE, TITLE, JavaUltimateIcons.Cdi.Gutter.BeanFactory, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val fileIndex = ProjectFileIndex.getInstance(module.project)

        return TapestryModuleClasses.of(module)
            .filter { moduleClass ->
                moduleClass.containingFile?.virtualFile?.let { fileIndex.isInSourceContent(it) } == true
            }
            .flatMap { moduleClass -> ModuleBuilder(moduleClass).services }
            .sortedBy { it.id }
            .map { ServiceNode(it, module) as SimpleNode }
            .toTypedArray()
    }

    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name, TITLE)

    private companion object {
        const val TITLE = "Services"
    }
}
