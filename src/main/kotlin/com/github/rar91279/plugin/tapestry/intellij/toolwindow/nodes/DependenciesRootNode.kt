package com.github.rar91279.plugin.tapestry.intellij.toolwindow.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import javax.swing.tree.DefaultMutableTreeNode

/**
 * The root of the dependency tree.
 *
 * All labels of the dependency tree nodes are precomputed in their constructors: nodes are built
 * inside a read action, while `toString()` runs on the EDT during rendering where PSI is off limits.
 * The renderer and the navigation actions dispatch on the concrete node types, so each stays distinct.
 */
class DependenciesRootNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    private val label = element.name.orEmpty()

    init {
        add(InjectedPagesNode(element))
        add(EmbeddedComponentsNode(element))
        add(TemplatesNode(element))
        add(MessageCatalogNode(element))
        add(UsagesNode(element))
    }

    override fun toString() = label
}
