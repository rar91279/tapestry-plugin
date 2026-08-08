package com.github.rar91279.plugin.tapestry.intellij

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.events.TapestryEventsManager
import com.intellij.psi.PsiFile
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Feeds PSI changes into the per-module [TapestryEventsManager], which is what drives live refresh of the
 * tool window and the project-view pane. These events are *not* redundant with
 * `PsiModificationTracker.MODIFICATION_COUNT`: that invalidates caches, but nothing tells the UI to
 * repaint.
 *
 * Registered per project (see [TapestryProjectListenerActivity]) rather than through the application-level
 * `psi.treeChangeListener` extension point it used to use — as an application listener it ran
 * `findModuleForPsiElement` + `isTapestryModule` on the write thread for every keystroke in every open
 * project, Tapestry or not.
 *
 * Content changes are debounced: `childAdded`/`childRemoved` fire on essentially every keystroke, and each
 * one previously triggered a full documentation re-render and a project-view `updateFromRoot`.
 */
@Service(Service.Level.PROJECT)
internal class TapestryPsiTreeChangeListener(
    private val project: Project,
    scope: CoroutineScope,
) : PsiTreeChangeAdapter(), Disposable {

    private class ContentChange(val events: TapestryEventsManager, val resource: PsiFile)

    private val contentChanges = MutableSharedFlow<ContentChange>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** The in-flight debounced refresh per edited file. Entries clear themselves when the refresh ends. */
    private val pendingRefreshes = ConcurrentHashMap<PsiFile, Job>()

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(this, this)

        scope.launch {
            contentChanges.collect { change ->
                // Debounced per file, not globally: a newer edit to the *same* file supersedes its pending
                // refresh, but an edit anywhere else must not cancel it. A single `collectLatest` over the
                // shared flow did cancel it, so touching a second file within the debounce window silently
                // dropped the refresh for the first — including the one the documentation tab is showing.
                pendingRefreshes.remove(change.resource)?.cancel()

                val refresh = launch {
                    delay(CONTENT_CHANGE_DEBOUNCE_MS)
                    // Consumers repaint Swing components, so the notification belongs on the EDT.
                    withContext(Dispatchers.EDT) { change.events.fileContentsChanged(change.resource) }
                }

                pendingRefreshes[change.resource] = refresh
                // Two-argument remove, so a refresh that has already been superseded doesn't evict its successor.
                refresh.invokeOnCompletion { pendingRefreshes.remove(change.resource, refresh) }
            }
        }
    }

    override fun dispose() = Unit

    override fun childRemoved(event: PsiTreeChangeEvent) {
        val events = tapestryProject(event)?.eventsManager ?: return

        when (val child = event.child) {
            is PsiClassOwner -> IdeaUtils.findPublicClass(child)?.let { events.classDeleted(it.qualifiedName) }
            is PsiFile -> child.virtualFile?.let { events.fileDeleted(it.path) }
            is PsiDirectory -> events.fileDeleted(child.virtualFile.path)
        }

        event.file?.let { contentChanges.tryEmit(ContentChange(events, it)) }
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        val events = tapestryProject(event)?.eventsManager ?: return

        event.file?.let { contentChanges.tryEmit(ContentChange(events, it)) }

        val added = event.child as? PsiFile ?: return
        // A new class file invalidates the whole model; a plain file only adds itself.
        if (added is PsiClassOwner) events.classCreated(null)
        else added.virtualFile?.let { events.fileCreated(it.path) }
    }

    /** The Tapestry project owning the changed element, or null if the change is outside a Tapestry module. */
    private fun tapestryProject(event: PsiTreeChangeEvent): TapestryProject? {
        val parent = event.parent ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(parent) ?: return null
        if (module.isDisposed || module.project != project || !TapestryUtils.isTapestryModule(module)) return null
        return TapestryModuleSupportLoader.getTapestryProject(module)
    }

    private companion object {
        const val CONTENT_CHANGE_DEBOUNCE_MS = 300L
    }
}
