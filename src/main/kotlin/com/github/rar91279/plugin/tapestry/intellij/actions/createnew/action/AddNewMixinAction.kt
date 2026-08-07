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
import com.github.rar91279.plugin.tapestry.intellij.actions.createnew.AddNewMixinDialog
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.util.Validators
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.MixinsNode

/**
 * Action that creates a new Tapestry mixin.
 *
 * This action displays a dialog allowing users to create a new Tapestry mixin
 * with a Java class file. The action validates the mixin name, manages the source
 * directory for the mixin class, and creates the mixin file in the appropriate location.
 *
 * The action is typically triggered from the project tree view on a [MixinsNode].
 */
class AddNewMixinAction : AddNewElementAction<MixinsNode>(MixinsNode::class.java) {

    /**
     * Returns the root package for mixins in the given Tapestry project.
     *
     * @param tapestryProject the Tapestry project to get the mixins root package from
     * @return the mixins root package path, or null if not configured
     */
    override fun getElementsRootPackage(tapestryProject: TapestryProject): String? =
        tapestryProject.mixinsRootPackage

    /**
     * Performs the action to create a new Tapestry mixin.
     *
     * This method:
     * - Opens a dialog for entering mixin details (name, source directory, file replacement options)
     * - Validates the mixin name
     * - Persists the selected source directory to module state
     * - Creates the mixin class file
     * - Handles errors and displays appropriate messages to the user
     *
     * @param event the action event that triggered this action, containing module and context information
     */
    override fun actionPerformed(event: AnActionEvent) {
        val module = event.getData(PlatformCoreDataKeys.MODULE) ?: return
        val defaultMixinPath = getDefaultElementPath(event, module) ?: return

        val dialog = AddNewMixinDialog(module, defaultMixinPath)
        val builder = DialogBuilder(module.project)
        builder.setCenterPanel(dialog.contentPane)
        builder.setTitle("New Tapestry Mixin")
        builder.setPreferredFocusComponent(dialog.nameComponent)

        builder.setOkOperation {
            val mixinName = dialog.newMixinName
            if (!Validators.isValidComponentName(mixinName)) {
                Messages.showErrorDialog("Invalid mixin name!", CommonBundle.getErrorTitle())
                return@setOkOperation
            }

            TapestryModuleSupportLoader.getInstance(module).state.newMixinsClassesSourceDirectory = dialog.classSourceDirectory.path

            ApplicationManager.getApplication().runWriteAction(Runnable {
                try {
                    val classSourceDirectory = PsiManager.getInstance(module.project).findDirectory(dialog.classSourceDirectory) ?: return@Runnable
                    TapestryUtils.createMixin(module, classSourceDirectory, mixinName, dialog.isReplaceExistingFiles)
                } catch (ex: IllegalStateException) {
                    Messages.showWarningDialog(module.project, ex.message, "Error Creating Mixin")
                }
            })
            builder.window.dispose()
        }

        builder.showModal(true)
    }
}
