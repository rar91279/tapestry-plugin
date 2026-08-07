package com.github.rar91279.plugin.tapestry.core.events

import com.github.rar91279.plugin.tapestry.core.resource.IResource

/**
 * An adapter class that provides default no-op implementations for all [FileSystemListener] methods.
 *
 * This class simplifies the implementation of file system listeners by allowing subclasses
 * to override only the methods they are interested in, rather than being forced to implement
 * all methods defined in the [FileSystemListener] interface.
 *
 * Subclasses should extend this class and override only the methods corresponding to the
 * file system events they want to handle.
 *
 * @see FileSystemListener
 */
abstract class FileSystemListenerAdapter : FileSystemListener {

    /**
     * Called when a file is created in the file system.
     *
     * Default implementation does nothing. Override this method to handle file creation events.
     *
     * @param path the path of the created file, or `null` if the path is unknown
     */
    override fun fileCreated(path: String?) {}

    /**
     * Called when a file is deleted from the file system.
     *
     * Default implementation does nothing. Override this method to handle file deletion events.
     *
     * @param path the path of the deleted file, or `null` if the path is unknown
     */
    override fun fileDeleted(path: String?) {}

    /**
     * Called when a class is created in the project.
     *
     * Default implementation does nothing. Override this method to handle class creation events.
     *
     * @param classFqn the fully qualified name of the created class, or `null` if the name is unknown
     */
    override fun classCreated(classFqn: String?) {}

    /**
     * Called when a class is deleted from the project.
     *
     * Default implementation does nothing. Override this method to handle class deletion events.
     *
     * @param classFqn the fully qualified name of the deleted class, or `null` if the name is unknown
     */
    override fun classDeleted(classFqn: String?) {}

    /**
     * Called when the contents of a file have changed.
     *
     * Default implementation does nothing. Override this method to handle file content change events.
     *
     * @param changedFile the resource representing the file whose contents have changed
     */
    override fun fileContentsChanged(changedFile: IResource) {}
}
