package com.intellij.tapestry.intellij.actions.createnew

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.intellij.util.TapestryUtils
import com.intellij.tapestry.intellij.util.Validators
import com.intellij.tapestry.intellij.view.nodes.ComponentsNode

/**
 * Action that creates a new component.
 */
class AddNewComponentAction : AddNewElementAction<ComponentsNode>(ComponentsNode::class.java) {

    override fun getElementsRootPackage(tapestryProject: TapestryProject): String? =
        tapestryProject.componentsRootPackage

    override fun actionPerformed(event: AnActionEvent) {
        val module = event.getData(PlatformCoreDataKeys.MODULE) ?: return
        val defaultComponentPath = getDefaultElementPath(event, module) ?: return

        val dialog = AddNewComponentDialog(module, defaultComponentPath, false)
        val builder = DialogBuilder(module.project)
        builder.setCenterPanel(dialog.contentPane)
        builder.setTitle("New Tapestry Component")
        builder.setPreferredFocusComponent(dialog.nameComponent)

        builder.setOkOperation {
            val componentName = dialog.newComponentName
            if (!Validators.isValidComponentName(componentName)) {
                Messages.showErrorDialog("Invalid component name!", CommonBundle.getErrorTitle())
                return@setOkOperation
            }

            val state = TapestryModuleSupportLoader.getInstance(module).state!!
            state.newComponentsClassesSourceDirectory = dialog.classSourceDirectory.path
            state.newComponentsTemplatesSourceDirectory = dialog.templateSourceDirectory.path

            ApplicationManager.getApplication().runWriteAction(Runnable {
                try {
                    val classSourceDirectory = PsiManager.getInstance(module.project).findDirectory(dialog.classSourceDirectory) ?: return@Runnable
                    val templateSourceDirectory = PsiManager.getInstance(module.project).findDirectory(dialog.templateSourceDirectory)
                    if (dialog.isNotCreatingTemplate) {
                        TapestryUtils.createComponent(module, classSourceDirectory, null, componentName, dialog.isReplaceExistingFiles)
                    } else {
                        TapestryUtils.createComponent(module, classSourceDirectory, templateSourceDirectory, componentName, dialog.isReplaceExistingFiles)
                    }
                } catch (ex: IllegalStateException) {
                    Messages.showWarningDialog(module.project, ex.message, "Error creating page")
                }
            })
            builder.window.dispose()
        }

        builder.showModal(true)
    }
}
