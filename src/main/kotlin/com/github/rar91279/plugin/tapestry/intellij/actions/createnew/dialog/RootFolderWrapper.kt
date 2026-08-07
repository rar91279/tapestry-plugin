package com.github.rar91279.plugin.tapestry.intellij.actions.createnew.dialog

import com.intellij.javaee.web.WebRoot
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VirtualFile

/**
 * Combo-box item wrapping either a Java source folder or a web root, shown by its file path.
 *
 * This class provides a unified wrapper for displaying either [SourceFolder] (Java source roots)
 * or [WebRoot] (web application roots) in UI combo boxes. The wrapper presents the folder's
 * file path as its string representation.
 *
 * Exactly one of [javaRootFolder] or [webRootFolder] must be non-null.
 *
 * @property javaRootFolder the Java source folder being wrapped, or null if this wrapper represents a web root
 * @property webRootFolder the web root being wrapped, or null if this wrapper represents a Java source folder
 */
internal class RootFolderWrapper private constructor(
    private val javaRootFolder: SourceFolder?,
    private val webRootFolder: WebRoot?,
) {
    /**
     * Creates a wrapper for a Java source folder.
     *
     * @param javaRootFolder the Java source folder to wrap
     */
    constructor(javaRootFolder: SourceFolder) : this(javaRootFolder, null)

    /**
     * Creates a wrapper for a web root.
     *
     * @param webRootFolder the web root to wrap
     */
    constructor(webRootFolder: WebRoot) : this(null, webRootFolder)

    /**
     * The virtual file representing the wrapped folder.
     *
     * Returns the file from either the Java source folder or the web root, whichever is non-null.
     *
     * @throws NullPointerException if both [javaRootFolder] and [webRootFolder] are null
     */
    val folder: VirtualFile get() = (javaRootFolder?.file ?: webRootFolder?.file)!!

    /**
     * Returns the string representation of this wrapper.
     *
     * @return the absolute file path of the wrapped folder
     */
    override fun toString(): String = folder.path
}
