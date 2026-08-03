package com.intellij.tapestry.intellij

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.intellij.core.resource.IntellijResource
import com.intellij.tapestry.intellij.util.IdeaUtils
import com.intellij.tapestry.intellij.util.TapestryUtils

internal class TapestryPsiTreeChangeListener : PsiTreeChangeAdapter() {

    override fun childRemoved(event: PsiTreeChangeEvent) {
        val events = tapestryProject(event)?.eventsManager ?: return

        when (val child = event.child) {
            is PsiClassOwner -> IdeaUtils.findPublicClass(child)?.let { events.classDeleted(it.qualifiedName) }
            is PsiFile -> child.virtualFile?.let { events.fileDeleted(it.path) }
            is PsiDirectory -> child.virtualFile?.let { events.fileDeleted(it.path) }
        }

        event.file?.let { events.fileContentsChanged(IntellijResource(it)) }
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        val events = tapestryProject(event)?.eventsManager ?: return

        event.file?.let { events.fileContentsChanged(IntellijResource(it)) }

        val added = event.child as? PsiFile ?: return
        // A new class file invalidates the whole model; a plain file only adds itself.
        if (added is PsiClassOwner) events.classCreated(null)
        else added.virtualFile?.let { events.fileCreated(it.path) }
    }

    /** The Tapestry project owning the changed element, or null if the change is outside a Tapestry module. */
    private fun tapestryProject(event: PsiTreeChangeEvent): TapestryProject? {
        val parent = event.parent ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(parent) ?: return null
        if (module.isDisposed || !TapestryUtils.isTapestryModule(module)) return null
        return TapestryModuleSupportLoader.getTapestryProject(module)
    }
}
