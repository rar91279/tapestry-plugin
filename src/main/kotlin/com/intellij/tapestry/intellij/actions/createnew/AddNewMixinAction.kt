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
import com.intellij.tapestry.intellij.view.nodes.MixinsNode

/**
 * Action that creates a new mixin.
 */
class AddNewMixinAction : AddNewElementAction<MixinsNode>(MixinsNode::class.java) {

    override fun getElementsRootPackage(tapestryProject: TapestryProject): String? =
        tapestryProject.mixinsRootPackage

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

            TapestryModuleSupportLoader.getInstance(module).state!!.newMixinsClassesSourceDirectory = dialog.classSourceDirectory.path

            ApplicationManager.getApplication().runWriteAction(Runnable {
                try {
                    val classSourceDirectory = PsiManager.getInstance(module.project).findDirectory(dialog.classSourceDirectory)
                    TapestryUtils.createMixin(module, classSourceDirectory, mixinName, dialog.isReplaceExistingFiles)
                } catch (ex: IllegalStateException) {
                    Messages.showWarningDialog(module.project, ex.message, "Error creating mixin")
                }
            })
            builder.window.dispose()
        }

        builder.showModal(true)
    }
}
