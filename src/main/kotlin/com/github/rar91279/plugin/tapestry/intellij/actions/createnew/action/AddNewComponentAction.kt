package com.github.rar91279.plugin.tapestry.intellij.actions.createnew.action

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.actions.createnew.dialog.AddNewComponentDialog
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.util.Validators
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.ComponentsNode

/**
 * Action that creates a new Tapestry component.
 *
 * This action displays a dialog allowing users to create a new Tapestry component
 * with both a Java class and an optional template file. The action validates the
 * component name, manages source directories for classes and templates, and creates
 * the component files in the appropriate locations.
 *
 * The action is typically triggered from the project tree view on a [ComponentsNode].
 */
class AddNewComponentAction : AddNewElementAction<ComponentsNode>(ComponentsNode::class.java) {

    /**
     * Returns the root package for components in the given Tapestry project.
     *
     * @param tapestryProject the Tapestry project to get the components root package from
     * @return the components root package path, or null if not configured
     */
    override fun getElementsRootPackage(tapestryProject: TapestryProject): String? =
        tapestryProject.componentsRootPackage

    /**
     * Performs the action to create a new Tapestry component.
     *
     * This method:
     * - Opens a dialog for entering component details (name, source directories, template options)
     * - Validates the component name
     * - Persists the selected source directories to module state
     * - Creates the component class and optional template file
     * - Handles errors and displays appropriate messages to the user
     *
     * @param event the action event that triggered this action, containing module and context information
     */
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

            val state = TapestryModuleSupportLoader.getInstance(module).state
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
                    Messages.showWarningDialog(module.project, ex.message, "Error Creating Page")
                }
            })
            builder.window.dispose()
        }

        builder.showModal(true)
    }
}
