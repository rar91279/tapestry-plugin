package com.github.rar91279.plugin.tapestry.intellij.actions.navigation

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiClassOwner
import com.github.rar91279.plugin.tapestry.core.exceptions.NotTapestryElementException
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.psi.PsiFile
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils

/**
 * Allows navigation to all templates of the class.
 */
class TemplatesNavigation : ActionGroup(), DumbAware {

    override fun update(event: AnActionEvent) {
        event.presentation.setEnabledAndVisible(
            TapestryUtils.isTapestryModule(event.getData(PlatformCoreDataKeys.MODULE)))
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        if (event == null) return EMPTY_ARRAY

        val psiFile = ClassTemplateNavigation.getEventPsiFile(event) ?: return EMPTY_ARRAY

        if (psiFile is PsiClassOwner && event.presentation.text == "Tapestry Template") {
            val tapestryElement: PresentationLibraryElement?
            try {
                val psiClass = IdeaUtils.findPublicClass(psiFile) ?: return EMPTY_ARRAY
                val module = event.getData(PlatformCoreDataKeys.MODULE) ?: return EMPTY_ARRAY
                tapestryElement = PresentationLibraryElement.createProjectElementInstance(
                    psiClass,
                    TapestryModuleSupportLoader.getTapestryProject(module))
            } catch (ex: NotTapestryElementException) {
                return EMPTY_ARRAY
            }

            if (tapestryElement == null || !tapestryElement.allowsTemplate()) return EMPTY_ARRAY

            return tapestryElement.template.map { TemplateNavigate(tapestryElement, it) }.toTypedArray()
        }
        return EMPTY_ARRAY
    }

    private class TemplateNavigate(private val tapestryElement: PresentationLibraryElement, template: PsiFile) :
        AnAction(template.name.replace("_", "__"), template.name, null) {

        override fun actionPerformed(event: AnActionEvent) {
            val project = event.getData(CommonDataKeys.PROJECT) ?: return
            for (template in tapestryElement.template) {
                if (template.name == event.presentation.description) {
                    FileEditorManager.getInstance(project)
                        .openFile(template.virtualFile, true)
                    return
                }
            }
        }
    }
}
