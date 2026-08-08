package com.github.rar91279.plugin.tapestry.intellij.actions.navigation

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.currentPsiFileInEditor
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.lang.TmlFileType

/**
 * Allows navigation from a class to its corresponding template and vice-versa.
 */
class ClassTemplateNavigation : AnAction() {

    override fun update(event: AnActionEvent) {
        val presentation = event.presentation

        val module = try {
            event.getData(PlatformCoreDataKeys.MODULE)
        } catch (ex: Throwable) {
            // Action update runs on the BGT and is routinely cancelled.
            if (ex is ControlFlowException) throw ex
            presentation.setEnabledAndVisible(false)
            return
        }

        if (!TapestryUtils.isTapestryModule(module)) {
            presentation.setEnabledAndVisible(false)
            return
        }

        val psiFile = getEventPsiFile(event)
        if (psiFile == null ||
            psiFile.fileType != TmlFileType && "Tapestry Class" == event.presentation.text) {
            presentation.isEnabled = false
            return
        }

        presentation.setEnabledAndVisible(true)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT)
        val psiFile = getEventPsiFile(event)
        val module = event.getData(PlatformCoreDataKeys.MODULE)
        if (psiFile == null || module == null) return

        val navigationTarget = findNavigationTarget(psiFile, module, event.presentation.text)
        if (navigationTarget != null) {
            FileEditorManager.getInstance(project!!).openFile(navigationTarget, true)
        } else {
            showCantNavigateMessage()
        }
    }

    companion object {

        fun findNavigationTarget(psiFile: PsiFile, module: Module, presentationText: String?): VirtualFile? {
            val project = TapestryModuleSupportLoader.getTapestryProject(module) ?: return null

            if (psiFile is PsiClassOwner && presentationText == "Class <-> Template Navigation") {
                val psiClass = IdeaUtils.findPublicClass(psiFile) ?: return null
                val tapestryElement = PresentationLibraryElement.createProjectElementInstance(
                    psiClass, project) ?: return null
                if (!tapestryElement.allowsTemplate()) return null
                val templates = tapestryElement.templateConsiderSuperClass
                return if (templates.isNotEmpty() && templates[0] != null)
                    templates[0].virtualFile else null
            }

            if (psiFile.fileType == TmlFileType &&
                (presentationText == "Class <-> Template Navigation" || presentationText == "Tapestry Class")) {
                val template = project.findElementByTemplate(psiFile) ?: return null
                return template.elementClass?.containingFile?.virtualFile
            }
            return null
        }

        /** Finds the PsiFile on which the event occurred, or `null` if it couldn't be determined. */
        fun getEventPsiFile(event: AnActionEvent): PsiFile? {
            val project = event.getData(CommonDataKeys.PROJECT) ?: return null
            return currentPsiFileInEditor(project)
        }

        fun showCantNavigateMessage() {
            Messages.showInfoMessage("Couldn't find a file to navigate to.", "Not Tapestry file")
        }
    }
}
