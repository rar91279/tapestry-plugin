package com.intellij.tapestry.intellij.actions.navigation

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.tapestry.core.model.presentation.TapestryComponent
import com.intellij.tapestry.intellij.toolwindow.TapestryToolWindowFactory
import com.intellij.tapestry.intellij.util.TapestryUtils

/**
 * Allows navigation from a tag to its corresponding documentation.
 */
class TagDocumentationNavigation : AnAction() {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getTapestryComponent(e) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val module = event.getData(PlatformCoreDataKeys.MODULE)

        val component = getTapestryComponent(event) ?: return

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TapestryToolWindowFactory.TAPESTRY_TOOLWINDOW_ID)
        val metatoolWindow = TapestryToolWindowFactory.getToolWindow(project)!!

        if (!metatoolWindow.mainPanel.isDisplayable && toolWindow != null) {
            toolWindow.show(null)
        }

        metatoolWindow.update(module, component, listOf(component.elementClass))
    }

    companion object {
        private fun getTapestryComponent(event: AnActionEvent): TapestryComponent? {
            val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
            val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return null

            val caretOffset = editor.caretModel.offset
            val tag = PsiTreeUtil.getParentOfType(psiFile.findElementAt(caretOffset), XmlTag::class.java)

            if (TapestryUtils.getComponentIdentifier(tag) == null) return null

            return TapestryUtils.getTypeOfTag(tag)
        }
    }
}
