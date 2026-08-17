package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.util.PathUtils
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDirectory
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.PlatformIcons

/** An element and the part of its name still to be consumed by the folders below the current node. */
internal class ElementEntry(val path: String, val element: PresentationLibraryElement)

/** Builds an element node for one element — the category decides which kind. */
internal typealias ElementNodeFactory = (PresentationLibraryElement, Module) -> SimpleNode

/**
 * A subpackage inside an element category, holding the elements named below it.
 *
 * Only ever created for a subpackage some element actually lives in, which is what keeps the tree free of the
 * empty and non-Tapestry directories the old package walk showed.
 */
class ElementFolderNode internal constructor(
    name: String,
    module: Module,
    directories: List<PsiDirectory>,
    private val entries: List<ElementEntry>,
    private val elementNode: ElementNodeFactory
) : DirectoryNode(module, directories) {

    init {
        init(directory ?: name, PresentationData(name, name, PlatformIcons.PACKAGE_ICON, null))
    }

    override fun getChildren(): Array<SimpleNode> = elementNodes(entries, directories, module, elementNode)
}

/**
 * The nodes for [entries] at one level: an element node for each element named directly here, a folder node for
 * each subpackage that holds any. Folders first, both halves sorted by name.
 */
internal fun elementNodes(
    entries: List<ElementEntry>,
    directories: List<PsiDirectory>,
    module: Module,
    elementNode: ElementNodeFactory
): Array<SimpleNode> {
    val separator = PathUtils.TAPESTRY_PATH_SEPARATOR
    val (nested, own) = entries.partition { it.path.contains(separator) }

    val folders = nested
        .groupBy { it.path.substringBefore(separator) }
        .toSortedMap()
        .map { (folder, inFolder) ->
            val below = inFolder.map { ElementEntry(it.path.substringAfter(separator), it.element) }
            ElementFolderNode(
                folder, module, directories.mapNotNull { it.findSubdirectory(folder) }, below, elementNode
            ) as SimpleNode
        }

    val elements = own
        .sortedBy { it.path }
        .map { elementNode(it.element, module) }

    return (folders + elements).toTypedArray()
}
