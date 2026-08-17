package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDirectory

/**
 * A node that stands for a place in the module where Tapestry elements live.
 *
 * The tree shows Tapestry entities, not directories, but a node still has to be able to answer "which
 * directory is this?": *New > Page/Component/Mixin* is enabled by finding such a node in the selection, and
 * *Safe Delete* collects a whole subtree from one. [directories] is a list because a package is spread over the
 * source roots that contribute to it — classes in `src/main/java`, templates and catalogs in
 * `src/main/resources`.
 */
abstract class DirectoryNode(module: Module, val directories: List<PsiDirectory>) : TapestryNode(module) {

    /** The directory this node is anchored on: the first source root that contributes to the package. */
    val directory: PsiDirectory? get() = directories.firstOrNull()

    /** The subdirectories named [name] across every directory of this node. */
    protected fun subdirectories(name: String): List<PsiDirectory> =
        directories.mapNotNull { it.findSubdirectory(name) }
}
