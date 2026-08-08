package com.github.rar91279.plugin.tapestry.core.events

import com.intellij.psi.PsiFile
import com.intellij.openapi.Disposable
import com.intellij.util.EventDispatcher
import org.jetbrains.annotations.TestOnly

/**
 * Central event manager that coordinates and distributes file system and Tapestry model change events.
 *
 * This class acts as both a listener and a broadcaster:
 * - As a listener: implements [FileSystemListener] and [TapestryModelChangeListener] to receive events from the IDE
 * - As a broadcaster: forwards those events to every registered listener
 *
 * Lifecycle: a manager lives on the module's [com.github.rar91279.plugin.tapestry.core.TapestryProject] and so
 * outlives the UI components that listen to it. Registration therefore takes a parent [Disposable] and the
 * listener is dropped automatically when that parent is disposed — a listener that unregistered itself by hand
 * (or forgot to) kept receiving events on a dead component.
 *
 * Registration is idempotent: re-registering an already-present listener is a no-op, so callers reacting to
 * module changes can simply re-run their subscription pass.
 *
 * Implementation note: Each IDE-specific implementation must register this class as a file system listener
 * with the platform's VFS and invoke the appropriate methods when file system events occur.
 *
 * @see FileSystemListener
 * @see TapestryModelChangeListener
 */
class TapestryEventsManager : FileSystemListener, TapestryModelChangeListener {

    private val fileSystemListeners = EventDispatcher.create(FileSystemListener::class.java)
    private val modelChangeListeners = EventDispatcher.create(TapestryModelChangeListener::class.java)

    /**
     * Registers a listener to receive notifications when the Tapestry model changes, until [parent] is disposed.
     * Does nothing if the listener is already registered.
     */
    fun addTapestryModelListener(listener: TapestryModelChangeListener, parent: Disposable) {
        if (listener in modelChangeListeners.listeners) return
        modelChangeListeners.addListener(listener, parent)
    }

    /**
     * Registers a listener to receive notifications about file system events, until [parent] is disposed.
     * Does nothing if the listener is already registered.
     */
    fun addFileSystemListener(listener: FileSystemListener, parent: Disposable) {
        if (listener in fileSystemListeners.listeners) return
        fileSystemListeners.addListener(listener, parent)
    }

    @TestOnly
    fun hasFileSystemListener(listener: FileSystemListener): Boolean = listener in fileSystemListeners.listeners

    @TestOnly
    fun hasTapestryModelListener(listener: TapestryModelChangeListener): Boolean =
        listener in modelChangeListeners.listeners

    /** Broadcasts file creation event to all registered file system listeners. */
    override fun fileCreated(path: String?) = fileSystemListeners.multicaster.fileCreated(path)

    /** Broadcasts file deletion event to all registered file system listeners. */
    override fun fileDeleted(path: String?) = fileSystemListeners.multicaster.fileDeleted(path)

    /** Broadcasts class creation event to all registered file system listeners. */
    override fun classCreated(classFqn: String?) = fileSystemListeners.multicaster.classCreated(classFqn)

    /** Broadcasts class deletion event to all registered file system listeners. */
    override fun classDeleted(classFqn: String?) = fileSystemListeners.multicaster.classDeleted(classFqn)

    /** Broadcasts file content change event to all registered file system listeners. */
    override fun fileContentsChanged(changedFile: PsiFile) =
        fileSystemListeners.multicaster.fileContentsChanged(changedFile)

    /** Broadcasts Tapestry model change event to all registered model change listeners. */
    override fun modelChanged() = modelChangeListeners.multicaster.modelChanged()
}
