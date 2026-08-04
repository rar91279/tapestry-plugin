package com.github.rar91279.plugin.tapestry.intellij.actions.createnew

import com.intellij.javaee.web.WebRoot
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VirtualFile

/** Combo-box item wrapping either a Java source folder or a web root, shown by its file path. */
internal class RootFolderWrapper private constructor(
    private val javaRootFolder: SourceFolder?,
    private val webRootFolder: WebRoot?,
) {
    constructor(javaRootFolder: SourceFolder) : this(javaRootFolder, null)
    constructor(webRootFolder: WebRoot) : this(null, webRootFolder)

    val folder: VirtualFile get() = (javaRootFolder?.file ?: webRootFolder?.file)!!

    override fun toString(): String = folder.path
}
