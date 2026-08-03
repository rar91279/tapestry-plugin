package com.intellij.tapestry.intellij.toolwindow.nodes

import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.model.presentation.InjectedElement
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.tapestry.core.resource.IResource
import javax.swing.tree.DefaultMutableTreeNode

/**
 * The nodes of the dependency tree. All labels are precomputed in the constructor: nodes are built
 * inside a read action, while `toString()` runs on the EDT during rendering where PSI is off limits.
 * The renderer and the navigation actions dispatch on the concrete node types, so each stays distinct.
 */

/** Nothing to show for an element whose class file is gone. */
private val PresentationLibraryElement.hasClassFile: Boolean
    get() = elementClass.file != null

class DependenciesRootNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    private val label = element.name

    init {
        add(InjectedPagesNode(element))
        add(EmbeddedComponentsNode(element))
        add(TemplatesNode(element))
        add(MessageCatalogNode(element))
        add(UsagesNode(element))
    }

    override fun toString() = label
}

class EmbeddedComponentsNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        if (element.hasClassFile) {
            for (template in element.template) add(EmbeddedTemplateNode(template, element))
            for (embedded in element.embeddedComponents) {
                if (embedded.template == "class") add(EmbeddedComponentNode(embedded.element))
            }
        }
    }

    override fun toString() = "Embedded Components"
}

class EmbeddedTemplateNode(resource: IResource, element: PresentationLibraryElement) :
    DefaultMutableTreeNode(resource) {

    private val label = resource.name.split("." + resource.extension)[0]

    init {
        if (element.hasClassFile) {
            for (embedded in element.embeddedComponentsTemplate) {
                if (embedded.template == resource.name) add(EmbeddedComponentNode(embedded.element))
            }
        }
    }

    override fun toString() = label
}

class InjectedPagesNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        if (element.hasClassFile) {
            for (injected in element.injectedPages) add(InjectedPageNode(injected))
        }
    }

    override fun toString() = "Injected Pages"
}

sealed class InjectedElementNode(val injected: InjectedElement, private val label: String) :
    DefaultMutableTreeNode(injected) {

    override fun toString() = label
}

class EmbeddedComponentNode(injected: InjectedElement) :
    InjectedElementNode(injected, injected.elementId ?: "")

/** Injected pages have no element id (that's a component concept), so fall back to the page name. */
class InjectedPageNode(injected: InjectedElement) :
    InjectedElementNode(
        injected,
        injected.elementId?.takeIf { it.isNotEmpty() } ?: injected.element?.name ?: ""
    )

class TemplatesNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        for (template in element.template) add(ResourceLeafNode(template))
    }

    override fun toString() = "Templates"
}

class MessageCatalogNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        for (catalog in element.messageCatalog) add(ResourceLeafNode(catalog))
    }

    override fun toString() = "Message Catalog"
}

/** A navigable leaf wrapping a single resource file (template, message catalog, ...). */
class ResourceLeafNode(resource: IResource) : DefaultMutableTreeNode(resource) {

    private val label = resource.name

    override fun toString() = label
}

class UsagesNode(element: PresentationLibraryElement) : DefaultMutableTreeNode(element) {

    init {
        for (usage in element.project.findUsages(element)) add(UsageNode(usage.user(), usage.kind()))
    }

    override fun toString() = "Used By"
}

/** A navigable leaf for an element that embeds or injects the shown element. */
class UsageNode(user: PresentationLibraryElement, val kind: TapestryProject.UsageKind) :
    DefaultMutableTreeNode(user) {

    private val label = user.name

    override fun toString() = label
}
