package com.github.rar91279.plugin.tapestry.core.events

import com.github.rar91279.plugin.tapestry.core.resource.IResource
import java.util.concurrent.CopyOnWriteArrayList

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
