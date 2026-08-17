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
 * Action that enables bidirectional navigation between Tapestry component classes and their corresponding templates.
 * 
 * This action allows developers to quickly switch between:
 * - A Tapestry component class and its template file (.tml)
 * - A template file (.tml) and its corresponding component class
 * 
 * The action is only available in Tapestry-enabled modules and when the current file
 * is either a valid Tapestry class or template file.
 */
class ClassTemplateNavigation : AnAction() {

    /**
     * Updates the presentation state of this action based on the current context.
     * 
     * The action is enabled only when:
     * - The current module has Tapestry support enabled
     * - The current file is either a Tapestry component class or a template file
     * 
     * @param event the action event containing context information
     */
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

    /**
     * Specifies that action updates should run on the Background Thread (BGT).
     * 
     * @return [ActionUpdateThread.BGT] to ensure thread-safe execution
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Performs the navigation action when invoked by the user.
     * 
     * Attempts to find and open the corresponding file (class or template) in the editor.
     * If no corresponding file is found, displays an informational message to the user.
     * 
     * @param event the action event containing context information including the current file and module
     */
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

        /**
         * Finds the navigation target file for the given source file.
         * 
         * Determines whether to navigate from a class to its template or vice-versa based on
         * the file type and presentation text. For classes, it looks up the corresponding template
         * using the Tapestry project model. For templates, it finds the associated component class.
         * 
         * @param psiFile the source file from which to navigate
         * @param module the module containing the file
         * @param presentationText the action's presentation text, used to determine navigation direction
         * @return the virtual file to navigate to, or null if no corresponding file exists
         */
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

        /**
         * Retrieves the PSI file currently active in the editor when the action event occurred.
         * 
         * @param event the action event
         * @return the current PSI file in the editor, or null if no project context is available
         *         or no file is currently being edited
         */
        fun getEventPsiFile(event: AnActionEvent): PsiFile? {
            val project = event.getData(CommonDataKeys.PROJECT) ?: return null
            return currentPsiFileInEditor(project)
        }

        /**
         * Displays an informational message when navigation to a corresponding file is not possible.
         * 
         * This occurs when the current file is not a valid Tapestry component class or template,
         * or when no corresponding file exists in the project.
         */
        fun showCantNavigateMessage() {
            Messages.showInfoMessage("Couldn't find a file to navigate to.", "Not Tapestry file")
        }
    }
}
