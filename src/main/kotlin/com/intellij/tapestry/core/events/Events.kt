package com.intellij.tapestry.core.events

import com.intellij.tapestry.core.resource.IResource
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A file system listener.
 * Classes that want to be notified of filesystem event should implement this interface.
 */
interface FileSystemListener {

    fun fileCreated(path: String?)

    fun fileDeleted(path: String?)

    fun fileContentsChanged(changedFile: IResource)

    fun classCreated(classFqn: String?)

    fun classDeleted(classFqn: String?)
}

/**
 * A Tapestry model change listener.
 * Classes that want to be notified of any change in the Tapestry model should implement this interface.
 */
interface TapestryModelChangeListener {

    fun modelChanged()
}

/**
 * A helper class for creating Tapestry listeners.
 */
abstract class FileSystemListenerAdapter : FileSystemListener {

    override fun fileCreated(path: String?) {}

    override fun fileDeleted(path: String?) {}

    override fun classCreated(classFqn: String?) {}

    override fun classDeleted(classFqn: String?) {}

    override fun fileContentsChanged(changedFile: IResource) {}
}

/**
 * Manages the events from the file system.
 * Each IDE implementation must also register this class as a filesystem listener and call the appropriate method on each event.
 */
class TapestryEventsManager : FileSystemListener, TapestryModelChangeListener {

    private val fileSystemListeners = CopyOnWriteArrayList<FileSystemListener>()
    private val modelChangeListeners = CopyOnWriteArrayList<TapestryModelChangeListener>()

    fun addTapestryModelListener(listener: TapestryModelChangeListener) {
        modelChangeListeners.add(listener)
    }

    fun removeTapestryModelListener(listener: TapestryModelChangeListener): Boolean =
        modelChangeListeners.remove(listener)

    fun addFileSystemListener(listener: FileSystemListener) {
        fileSystemListeners.add(listener)
    }

    fun removeFileSystemListener(listener: FileSystemListener): Boolean = fileSystemListeners.remove(listener)

    override fun fileCreated(path: String?) = fileSystemListeners.forEach { it.fileCreated(path) }

    override fun fileDeleted(path: String?) = fileSystemListeners.forEach { it.fileDeleted(path) }

    override fun classCreated(classFqn: String?) = fileSystemListeners.forEach { it.classCreated(classFqn) }

    override fun classDeleted(classFqn: String?) = fileSystemListeners.forEach { it.classDeleted(classFqn) }

    override fun fileContentsChanged(changedFile: IResource) =
        fileSystemListeners.forEach { it.fileContentsChanged(changedFile) }

    override fun modelChanged() = modelChangeListeners.forEach { it.modelChanged() }
}
