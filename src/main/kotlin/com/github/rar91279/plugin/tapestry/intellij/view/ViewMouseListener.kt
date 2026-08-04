package com.github.rar91279.plugin.tapestry.intellij.view

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.github.rar91279.plugin.tapestry.core.exceptions.NotTapestryElementException
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.ComponentNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.MixinNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.PageNode
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.util.concurrency.AppExecutorUtil
import javax.swing.JComponent
import javax.swing.TransferHandler
import javax.swing.event.MouseInputAdapter
import javax.swing.tree.DefaultMutableTreeNode
import java.awt.event.MouseEvent
import kotlin.math.abs

internal class ViewMouseListener(private val viewPane: TapestryProjectViewPane) : MouseInputAdapter() {

    private var firstMouseEvent: MouseEvent? = null

    /**
     * Caches the presentation-element lookup for (file, module), keyed by identity. Resolving it
     * touches the stub index (via [PresentationLibraryElement.createProjectElementInstance] ->
     * [com.github.rar91279.plugin.tapestry.core.TapestryProject.getApplicationLibrary]), which the platform now
     * forbids running synchronously on the EDT. mousePressed fires on the EDT and must decide
     * synchronously whether to arm the drag, so the lookup itself runs in the background on a cache
     * miss; that one click doesn't start a drag, but the next click on the same file does.
     */
    private val dragTargetCache = HashMap<Pair<VirtualFile, Module>, PresentationLibraryElement?>()
    private val pendingLookups = HashSet<Pair<VirtualFile, Module>>()

    override fun mousePressed(event: MouseEvent) {
        // The cheap checks (node/file type, editor state) still need a read action for PSI access,
        // but none of them touch the index — safe to run synchronously.
        if (ReadAction.compute<Boolean, RuntimeException> { canStartDrag(event) }) {
            firstMouseEvent = event
            event.consume()
        }
    }

    private fun canStartDrag(event: MouseEvent): Boolean {
        val selectionPath = viewPane.tree.selectionPath ?: return false
        if (viewPane.tree.selectionPaths!!.size >= 2) return false

        val selectedNode = (selectionPath.lastPathComponent as DefaultMutableTreeNode).userObject

        // If dragged node isn't a Page or Component or Mixin don't drag
        if (selectedNode !is PageNode && selectedNode !is ComponentNode && selectedNode !is MixinNode) return false

        // A Page/Component/Mixin node always carries a module (see TapestryNode's constructor).
        val module: Module = viewPane.getSelectedModule()!!

        // If there's no file opened don't drag
        if (FileEditorManager.getInstance(viewPane.project).selectedFiles.isEmpty()) return false

        val fileInEditor = PsiManager.getInstance(viewPane.project)
            .findFile(FileDocumentManager.getInstance().getFile(FileEditorManager.getInstance(viewPane.project).selectedTextEditor!!.document)!!)
            ?: return false
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
            val presentationLibraryElement = resolveCached(fileInEditor.virtualFile, module, fileInEditor) ?: return false

            // If dropping on a page class: can only drop Pages and Components
            if (presentationLibraryElement.elementType == PresentationLibraryElement.ElementType.PAGE &&
                selectedNode !is PageNode && selectedNode !is ComponentNode) return false

            // If dropping on a component class: can only drop Pages, Components and Mixins
            if (presentationLibraryElement.elementType == PresentationLibraryElement.ElementType.COMPONENT &&
                selectedNode !is PageNode && selectedNode !is ComponentNode && selectedNode !is MixinNode) return false

            // If dropping on a mixin class: can only drop Pages
            if (presentationLibraryElement.elementType == PresentationLibraryElement.ElementType.MIXIN &&
                selectedNode !is PageNode) return false
        }

        return true
    }

    /**
     * Returns the cached presentation element for (file, module), or `null` on a cache miss —
     * kicking off a background lookup so the *next* click on this file has an answer.
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
                    IntellijJavaClassType(module, psiClass.containingFile),
                    TapestryModuleSupportLoader.getTapestryProject(module)!!)
            } catch (e: NotTapestryElementException) {
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

    override fun mouseReleased(event: MouseEvent) {
        firstMouseEvent = null
    }
}
