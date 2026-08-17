package com.github.rar91279.plugin.tapestry.intellij.view

import com.github.rar91279.plugin.tapestry.core.exceptions.NotTapestryElementException
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.util.currentPsiFileInEditor
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.ComponentNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.MixinNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.PageNode
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.xml.XmlFile
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.TransferHandler
import javax.swing.event.MouseInputAdapter
import kotlin.math.abs

/**
 * Handles mouse events for initiating drag-and-drop operations from the Tapestry project view tree.
 *
 * This listener allows dragging Pages, Components, and Mixins from the project structure tree into
 * open editor files (Java classes or TML templates). It validates drag sources and drop targets
 * according to Tapestry element compatibility rules and enforces write access constraints.
 *
 * **Threading model:**
 * - [mousePressed] runs on the EDT and must decide synchronously whether to arm a drag
 * - Presentation element resolution (via stub index) runs asynchronously in the background
 * - The first click on a new file triggers a background lookup; subsequent clicks use cached results
 *
 * @property viewPane The Tapestry project view pane that owns this listener
 */
internal class ViewMouseListener(private val viewPane: TapestryProjectViewPane) : MouseInputAdapter() {

    /**
     * Stores the initial mouse event when a potential drag operation begins.
     * Used to calculate drag threshold distance and initiate the TransferHandler drag.
     * Reset to null when the drag completes or is cancelled.
     */
    private var firstMouseEvent: MouseEvent? = null

    /**
     * Caches presentation element lookups by (file, module) identity to avoid EDT index access.
     *
     * **Why caching is necessary:**
     * Resolving a [PresentationLibraryElement] touches the stub index (via
     * [PresentationLibraryElement.createProjectElementInstance] →
     * [com.github.rar91279.plugin.tapestry.core.TapestryProject.getApplicationLibrary]), which the
     * IntelliJ platform forbids running synchronously on the EDT.
     *
     * **Threading behavior:**
     * - [mousePressed] fires on the EDT and must decide synchronously whether to arm a drag
     * - On a cache miss, the lookup runs asynchronously in the background
     * - That first click does not start a drag, but subsequent clicks on the same file do
     *
     * **Cache entries:**
     * - `key → element`: successful lookup
     * - `key → null`: file is not a Tapestry presentation element (NotTapestryElementException)
     * - key absent: no lookup has completed yet
     */
    private val dragTargetCache = HashMap<Pair<VirtualFile, Module>, PresentationLibraryElement?>()

    /**
     * Tracks background lookups currently in flight to prevent duplicate resolution requests.
     * A key is added when a background lookup is submitted and removed when it completes.
     */
    private val pendingLookups = HashSet<Pair<VirtualFile, Module>>()

    /**
     * Determines whether a mouse press should arm a drag operation.
     *
     * Runs synchronous validation checks under a read action (node type, file type, editor state,
     * write access). These checks do **not** touch the stub index, so they are safe to run on the EDT.
     *
     * Uses `ReadAction.computeBlocking` (not `computeCancellable`) because the latter asserts a
     * background thread, and this method always runs on the EDT.
     *
     * @param event The mouse press event to evaluate
     */
    override fun mousePressed(event: MouseEvent) {
        // The cheap checks (node/file type, editor state) still need a read action for PSI access,
        // but none of them touch the index — safe to run synchronously.
        // computeBlocking, not computeCancellable: the latter asserts a background thread, and this is the EDT.
        if (ReadAction.computeBlocking<Boolean, RuntimeException> { canStartDrag(event) }) {
            firstMouseEvent = event
            event.consume()
        }
    }

    /**
     * Validates whether the current selection and editor state permit starting a drag operation.
     *
     * **Validation steps:**
     * 1. Exactly one tree node must be selected
     * 2. Selected node must be a PageNode, ComponentNode, or MixinNode
     * 3. An editor file must be open and writable
     * 4. Editor file must belong to the same module as the dragged element
     * 5. For TML files: must declare Tapestry namespace; cannot drag MixinNodes
     * 6. For Java files: drop target compatibility (Page/Component/Mixin class rules)
     *
     * **Must be called from a read action** due to PSI access.
     *
     * @param event The mouse event (currently unused but kept for potential extension)
     * @return `true` if a drag can be armed, `false` otherwise
     */
    private fun canStartDrag(event: MouseEvent): Boolean {
        val selectionPath = viewPane.tree.selectionPath ?: return false
        if (viewPane.tree.selectionPaths!!.size >= 2) return false

        // Same reason as everywhere else this tree is read: a tree restoring itself from a cached presentation
        // hands out placeholder nodes, and a hard cast throws on the first click after startup.
        val selectedNode = IdeaUtils.nodeOf(selectionPath)?.userObject

        // If dragged node isn't a Page or Component or Mixin don't drag
        if (selectedNode !is PageNode && selectedNode !is ComponentNode && selectedNode !is MixinNode) return false

        // A Page/Component/Mixin node always carries a module (see TapestryNode's constructor).
        val module: Module = viewPane.getSelectedModule()!!

        // If there's no file opened don't drag
        if (FileEditorManager.getInstance(viewPane.project).selectedFiles.isEmpty()) return false

        val fileInEditor = currentPsiFileInEditor(viewPane.project) ?: return false
        val typeFileInEditor = fileInEditor.fileType

        // If the file in editor isn't either JAVA or TML don't drag
        if (fileInEditor !is PsiClassOwner && typeFileInEditor != TmlFileType) return false

        // If the file in the editor isn't writable or isn't part of the module where the drag is from don't drag
        val moduleForFile = ProjectRootManager.getInstance(viewPane.project).fileIndex.getModuleForFile(fileInEditor.virtualFile)
        if (!fileInEditor.isWritable || moduleForFile == null || moduleForFile != module) return false

        // If the file in the editor is a TML file
        if (typeFileInEditor == TmlFileType) {
            // If the file doesn't declare the Tapestry namespace don't drag
            if (TapestryUtils.getTapestryNamespacePrefix(fileInEditor as XmlFile) == null) return false

            // Don't drag mixins to templates
            if (selectedNode is MixinNode) return false
        }

        // If the file in the editor is a JAVA file
        if (fileInEditor is PsiClassOwner) {
            val presentationLibraryElement =
                resolveCached(fileInEditor.virtualFile, module, fileInEditor) ?: return false

            return when (presentationLibraryElement.elementType) {
                // If dropping on a page class: can only drop Pages and Components
                PresentationLibraryElement.ElementType.PAGE ->
                    selectedNode is PageNode || selectedNode is ComponentNode

                // If dropping on a component class: can only drop Pages, Components and Mixins
                PresentationLibraryElement.ElementType.COMPONENT ->
                    selectedNode is PageNode || selectedNode is ComponentNode || selectedNode is MixinNode

                // If dropping on a mixin class: can only drop Pages
                PresentationLibraryElement.ElementType.MIXIN ->
                    selectedNode is PageNode

                else -> false
            }
        }

        return true
    }

    /**
     * Returns the cached presentation element for (file, module), or `null` on a cache miss.
     *
     * **Cache-miss behavior:**
     * On a cache miss, schedules a background lookup using `ReadAction.nonBlocking` so that the
     * **next** click on this file has a cached answer. The current call returns `null`.
     *
     * **Cached null values:**
     * If a file is **not** a Tapestry presentation element (throws [NotTapestryElementException]),
     * the cache stores `null` to avoid repeated failed lookups.
     *
     * **Must be called from a read action** to access [psiClassOwner].
     *
     * @param file The virtual file being checked
     * @param module The module context for the lookup
     * @param psiClassOwner The PSI representation of the file (must be a Java class file)
     * @return The cached [PresentationLibraryElement], or `null` if not cached or not a Tapestry element
     */
    private fun resolveCached(file: VirtualFile, module: Module, psiClassOwner: PsiClassOwner): PresentationLibraryElement? {
        val key = file to module
        dragTargetCache[key]?.let { return it }
        if (key in dragTargetCache) return null // cached "not a presentation element"
        if (!pendingLookups.add(key)) return null // lookup already in flight

        ReadAction.nonBlocking<PresentationLibraryElement?> {
            try {
                val psiClass = IdeaUtils.findPublicClass(psiClassOwner) ?: return@nonBlocking null
                PresentationLibraryElement.createProjectElementInstance(
                    psiClass,
                    TapestryModuleSupportLoader.getTapestryProject(module)!!)
            } catch (_: NotTapestryElementException) {
                null
            }
        }
            .expireWith(viewPane)
            .finishOnUiThread(ModalityState.any()) { element ->
                dragTargetCache[key] = element
                pendingLookups.remove(key)
            }
            .submit(AppExecutorUtil.getAppExecutorService())

        return null
    }

    /**
     * Detects when the mouse has moved enough to transition from a potential drag to an active drag.
     *
     * Uses a 5-pixel Manhattan distance threshold to distinguish drag gestures from accidental mouse
     * movement during clicks. Once the threshold is exceeded, delegates to the component's
     * [TransferHandler] to initiate the actual drag-and-drop operation.
     *
     * @param event The mouse drag event containing current cursor position
     */
    override fun mouseDragged(event: MouseEvent) {
        val first = firstMouseEvent ?: return
        event.consume()

        val dx = abs(event.x - first.x)
        val dy = abs(event.y - first.y)
        // Arbitrarily define a 5-pixel shift as the official beginning of a drag.
        if (dx > 5 || dy > 5) {
            // This is a drag, not a click.
            val component = event.source as JComponent
            // Tell the transfer handler to initiate the drag.
            component.transferHandler.exportAsDrag(component, first, TransferHandler.COPY)
            firstMouseEvent = null
        }
    }

    /**
     * Resets drag tracking when the mouse button is released.
     *
     * Clears [firstMouseEvent] to cancel any potential drag that didn't exceed the movement
     * threshold, or to clean up after a completed drag operation.
     *
     * @param event The mouse release event
     */
    override fun mouseReleased(event: MouseEvent) {
        firstMouseEvent = null
    }
}
