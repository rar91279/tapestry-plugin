package com.github.rar91279.plugin.tapestry.core.events

import com.github.rar91279.plugin.tapestry.core.resource.IResource

/**
 * A file system listener for monitoring file system events.
 *
 * Classes that want to be notified of file system events should implement this interface.
 * Implementations will receive notifications about file creation, deletion, content changes,
 * and class-level events within the project structure.
 */
interface FileSystemListener {

    /**
     * Called when a new file is created in the file system.
     *
     * @param path the absolute path of the created file, or null if the path is unavailable
     */
    fun fileCreated(path: String?)

    /**
     * Called when a file is deleted from the file system.
     *
     * @param path the absolute path of the deleted file, or null if the path is unavailable
     */
    fun fileDeleted(path: String?)

    /**
     * Called when the contents of a file have been modified.
     *
     * @param changedFile the resource representing the file whose contents have changed
     */
    fun fileContentsChanged(changedFile: IResource)

    /**
     * Called when a new class is created in the project.
     *
     * @param classFqn the fully qualified name of the created class, or null if unavailable
     */
    fun classCreated(classFqn: String?)

    /**
     * Called when a class is deleted from the project.
     *
     * @param classFqn the fully qualified name of the deleted class, or null if unavailable
     */
    fun classDeleted(classFqn: String?)
}
