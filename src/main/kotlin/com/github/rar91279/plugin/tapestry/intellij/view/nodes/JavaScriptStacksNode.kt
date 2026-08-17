package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.AssetKind
import com.github.rar91279.plugin.tapestry.core.model.presentation.MODULE_ROOT_PATH
import com.github.rar91279.plugin.tapestry.core.model.presentation.assetKind
import com.github.rar91279.plugin.tapestry.intellij.JavaScriptStack
import com.github.rar91279.plugin.tapestry.intellij.JavaScriptStackResolver
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import com.intellij.ui.treeStructure.SimpleNode

/** The JavaScript stacks the module contributes. */
class JavaScriptStacksNode(module: Module) : TapestryNode(module) {

    init {
        init(TITLE, PresentationData(TITLE, TITLE, AllIcons.Nodes.PpLib, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val project = TapestryModuleSupportLoader.getTapestryProject(module) ?: return NO_CHILDREN

        return JavaScriptStackResolver(module, project).resolveAll()
            .map { StackNode(it, module) as SimpleNode }
            .toTypedArray()
    }

    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name, TITLE)

    private companion object {
        const val TITLE = "JavaScriptStacks"
    }
}

/**
 * One contributed stack, under the class the contribution names, holding what it loads.
 *
 * The parts are split the way the stack itself declares them — stylesheets, libraries, modules — because that
 * is what a stack is: three ordered lists plus the stacks it depends on.
 */
class StackNode(private val stack: JavaScriptStack, module: Module) : TapestryNode(module) {

    init {
        init(stack.declaration, PresentationData(stack.name, stack.name, AllIcons.Nodes.Class, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val modules = stack.assets.filter { it.isJavaScriptModule() }
        val assets = stack.assets - modules.toSet()

        return listOfNotNull(
            partNode("Css", assets.filter { it.assetKind() == AssetKind.STYLESHEET }),
            partNode("JS", assets.filter { it.assetKind() == AssetKind.SCRIPT }),
            partNode("Modules", modules)
        ).toTypedArray()
    }

    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name, stack.name)

    /** A part is left out entirely when the stack has nothing of that kind — an empty group says nothing. */
    private fun partNode(title: String, files: List<PsiFile>): SimpleNode? =
        if (files.isEmpty()) null else StackPartNode(title, stack.name, files, module)

    private fun PsiFile.isJavaScriptModule(): Boolean =
        virtualFile?.path?.contains(MODULE_ROOT_PATH) == true
}

/** The stylesheets, libraries or modules of one stack. */
class StackPartNode(
    private val title: String,
    private val stackName: String,
    private val files: List<PsiFile>,
    module: Module
) : TapestryNode(module) {

    init {
        init("$stackName/$title", PresentationData(title, title, AllIcons.Nodes.Folder, null))
    }

    override fun getChildren(): Array<SimpleNode> =
        files.sortedBy { it.name }.map { FileNode(it, module) as SimpleNode }.toTypedArray()

    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name, stackName, title)
}
