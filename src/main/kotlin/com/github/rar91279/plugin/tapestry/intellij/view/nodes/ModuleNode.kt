package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.ui.treeStructure.SimpleNode

/**
 * A Tapestry module, shown as the fixed set of things a Tapestry application is made of.
 *
 * This used to walk the module's source content and mirror the package hierarchy, which put pages and
 * components among plain packages and plain classes. The categories are resolved from the model instead:
 * nothing that is not a Tapestry entity reaches the tree.
 *
 * @param options the toolbar toggles, read as the children are built — see [TapestryViewOptions].
 */
class ModuleNode(
    module: Module,
    private val options: TapestryViewOptions = TapestryViewOptions.DEFAULT
) : AbstractModuleNode(module) {

    override fun getChildren(): Array<SimpleNode> {
        val module = getValue() as Module
        val project = TapestryModuleSupportLoader.getTapestryProject(module) ?: return NO_CHILDREN
        val library = project.applicationLibrary
        val showFiles = options.showElementFiles

        val children = mutableListOf<SimpleNode>()

        // A branch that holds nothing is left out, categories included: a module that only provides IoC
        // services has no business showing empty Pages, Components and Mixins. *New > Page/Component/Mixin* is
        // enabled on the module node itself, so the first page of a module can still be created from here.
        children.addIfNotEmpty(ServicesNode(module))
        children.addIfNotEmpty(
            PagesNode(module, directoriesOf(project.pagesRootPackage), library?.pages?.values.orEmpty(), showFiles)
        )
        children.addIfNotEmpty(
            ComponentsNode(
                module, directoriesOf(project.componentsRootPackage), library?.components?.values.orEmpty(), showFiles
            )
        )
        children.addIfNotEmpty(
            MixinsNode(module, directoriesOf(project.mixinsRootPackage), library?.mixins?.values.orEmpty(), showFiles)
        )
        children.addIfNotEmpty(AssetsNode(module))

        if (options.showLibraries) children.addIfNotEmpty(LibrariesNode(module))

        return children.toTypedArray()
    }

    /**
     * Every directory that contributes to a package — a category package is spread over the source roots that
     * hold its classes and its templates.
     */
    private fun directoriesOf(packageName: String?): List<PsiDirectory> {
        val name = packageName ?: return emptyList()
        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)

        return JavaPsiFacade.getInstance(module.project).findPackage(name)?.getDirectories(scope)?.asList().orEmpty()
    }

    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name)
}

/**
 * Adds [node] unless it would show up empty.
 *
 * The children are built to find out, which is why this is used only for the branches worth hiding: it moves
 * their cost to the module node's own expansion.
 */
internal fun MutableList<SimpleNode>.addIfNotEmpty(node: SimpleNode) {
    if (node.children.isNotEmpty()) add(node)
}
