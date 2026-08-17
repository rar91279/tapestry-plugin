package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.AssetKind
import com.github.rar91279.plugin.tapestry.core.model.presentation.assetKind
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.ui.treeStructure.SimpleNode
import javax.swing.Icon

/**
 * One kind of asset under a module's asset roots — stylesheets, scripts, or the JavaScript modules.
 *
 * Everything that exists is listed, not only what some element imports: an asset nobody imports yet is exactly
 * what one goes looking for. Contents keep their folder structure, which in a real application is the
 * difference between a readable list and several hundred flat entries.
 *
 * @param kind the asset kind to show, or `null` to show every file — how the module root is listed, where the
 *             file's extension says nothing about what it is for.
 */
open class AssetCategoryNode(
    private val title: String,
    icon: Icon,
    module: Module,
    private val roots: List<PsiDirectory>,
    private val kind: AssetKind?
) : TapestryNode(module) {

    init {
        init(title, PresentationData(title, title, icon, null))
    }

    override fun getChildren(): Array<SimpleNode> = assetNodes(roots, kind, module)

    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name, title)
}

/** A folder inside an asset category, shown only when it holds an asset of the category's kind. */
class AssetFolderNode(
    private val name: String,
    module: Module,
    private val directories: List<PsiDirectory>,
    private val kind: AssetKind?
) : TapestryNode(module) {

    init {
        init(directories.first(), PresentationData(name, name, AllIcons.Nodes.Folder, null))
    }

    override fun getChildren(): Array<SimpleNode> = assetNodes(directories, kind, module)

    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name, name, directories.first().virtualFile.path)
}

/**
 * The nodes for one level of an asset tree: folders that hold something of this [kind] first, then the files of
 * that kind. Directories are merged across roots, so the same relative path contributed by two source roots
 * reads as one folder.
 */
internal fun assetNodes(directories: List<PsiDirectory>, kind: AssetKind?, module: Module): Array<SimpleNode> {
    val folders = directories
        .flatMap { it.subdirectories.asList() }
        .groupBy { it.name }
        .toSortedMap()
        .filterValues { candidates -> candidates.any { it.holdsAsset(kind) } }
        .map { (name, candidates) -> AssetFolderNode(name, module, candidates, kind) as SimpleNode }

    val files = directories
        .flatMap { it.files.asList() }
        .filter { it.isAsset(kind) }
        .distinctBy { it.name }
        .sortedBy { it.name }
        .map { FileNode(it, module) as SimpleNode }

    return (folders + files).toTypedArray()
}

private fun PsiFile.isAsset(kind: AssetKind?): Boolean = kind == null || assetKind() == kind

private fun PsiDirectory.holdsAsset(kind: AssetKind?): Boolean =
    files.any { it.isAsset(kind) } || subdirectories.any { it.holdsAsset(kind) }
