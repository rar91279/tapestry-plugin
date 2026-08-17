package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDirectory
import com.intellij.ui.treeStructure.SimpleNode
import javax.swing.Icon

/**
 * One of a module's element categories — Pages, Components, Mixins — holding the elements the model found,
 * nested by subpackage.
 *
 * The nesting comes from the element names rather than from a directory walk: the model names an element by its
 * path below the category package (`admin/Login`), so a subpackage holding no element never appears, and
 * neither does a class that is not a Tapestry element. The directories are carried along only so the actions
 * that need one keep working — see [DirectoryNode].
 */
abstract class ElementCategoryNode(
    private val title: String,
    icon: Icon,
    module: Module,
    directories: List<PsiDirectory>,
    private val elements: Collection<PresentationLibraryElement>,
    protected val showElementFiles: Boolean
) : DirectoryNode(module, directories) {

    init {
        init(directory ?: title, PresentationData(title, title, icon, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val entries = elements.mapNotNull { element ->
            element.name?.takeIf { it.isNotEmpty() }?.let { ElementEntry(it, element) }
        }

        return elementNodes(entries, directories, module, ::elementNode)
    }

    /** The node kind this category shows its elements as. */
    protected abstract fun elementNode(element: PresentationLibraryElement, module: Module): SimpleNode

    // The title, not the directory: two categories of the same module must not compare equal just because
    // their packages happen to resolve to the same directory (they don't, but node identity across refreshes
    // is not the place to rely on that).
    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name, title)
}
