package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.core.model.presentation.ASSET_ROOT_PATH
import com.github.rar91279.plugin.tapestry.core.model.presentation.AssetKind
import com.github.rar91279.plugin.tapestry.core.model.presentation.MODULE_ROOT_PATH
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.ui.treeStructure.SimpleNode
import javax.swing.Icon

/**
 * The client-side side of a module: its stylesheets, scripts, JavaScript modules and stacks.
 *
 * The contents come from the places Tapestry actually loads assets from — `META-INF/assets` and
 * `META-INF/modules` on every source and resource root, plus the web context roots — rather than from what the
 * elements happen to import.
 */
class AssetsNode(module: Module) : TapestryNode(module) {

    init {
        init(TITLE, PresentationData(TITLE, TITLE, AllIcons.Nodes.Folder, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val assetRoots = directoriesFor(ASSET_ROOT_PATH) + webRoots()
        val moduleRoots = directoriesFor(MODULE_ROOT_PATH)

        val children = mutableListOf<SimpleNode>()

        // A category with no file behind it is dropped: an application with no stylesheets of its own should
        // not have to look at an empty StyleSheets branch.
        children.addIfNotEmpty(
            AssetCategoryNode("StyleSheets", iconFor("asset.css"), module, assetRoots, AssetKind.STYLESHEET)
        )
        children.addIfNotEmpty(
            AssetCategoryNode("Javascripts", iconFor("asset.js"), module, assetRoots, AssetKind.SCRIPT)
        )
        // Every file, whatever its extension: under the module root, being there is what makes it a module.
        children.addIfNotEmpty(AssetCategoryNode("Modules", iconFor("module.js"), module, moduleRoots, null))
        children.addIfNotEmpty(JavaScriptStacksNode(module))

        return children.toTypedArray()
    }

    override fun getEqualityObjects(): Array<Any> = arrayOf(module.name, TITLE)

    /** The directories at [rootRelativePath] under any of the module's source and resource roots. */
    private fun directoriesFor(rootRelativePath: String): List<PsiDirectory> =
        ModuleRootManager.getInstance(module).getSourceRoots(false)
            .mapNotNull { it.findFileByRelativePath(rootRelativePath) }
            .mapNotNull { it.asPsiDirectory() }

    /** The web context roots, which hold the assets imported with a `context:` prefix. */
    private fun webRoots(): List<PsiDirectory> =
        IdeaUtils.getWebFacet(module)?.webRoots.orEmpty()
            .mapNotNull { it.file }
            .mapNotNull { it.asPsiDirectory() }

    private fun VirtualFile.asPsiDirectory(): PsiDirectory? =
        if (isDirectory) PsiManager.getInstance(module.project).findDirectory(this) else null

    /** The icon of the file type a category holds, so the branches read like the files under them. */
    private fun iconFor(fileName: String): Icon =
        FileTypeManager.getInstance().getFileTypeByFileName(fileName).icon ?: AllIcons.Nodes.Folder

    private companion object {
        const val TITLE = "Assets"
    }
}
