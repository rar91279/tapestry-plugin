package com.github.rar91279.plugin.tapestry.intellij.actions.createnew.action

import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.actions.createnew.dialog.AddNewComponentDialog
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.util.Validators
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.PagesNode

/**
 * Action that creates a new Tapestry page.
 *
 * This action displays a dialog allowing users to create a new Tapestry page
 * with both a Java class and an optional template file. The action validates the
 * page name, manages source directories for classes and templates, and creates
 * the page files in the appropriate locations.
 *
 * The action is typically triggered from the project tree view on a [PagesNode].
 */
class AddNewPageAction : AddNewElementAction<PagesNode>(PagesNode::class.java) {

    /**
     * Returns the root package for pages in the given Tapestry project.
     *
     * @param tapestryProject the Tapestry project to get the pages root package from
     * @return the pages root package path, or null if not configured
     */
    override fun getElementsRootPackage(tapestryProject: TapestryProject): String? =
        tapestryProject.pagesRootPackage

    /**
     * Performs the action to create a new Tapestry page.
     *
     * This method:
     * - Opens a dialog for entering page details (name, source directories, template options)
     * - Validates the page name
     * - Persists the selected source directories to module state
     * - Creates the page class and optional template file
     * - Handles errors and displays appropriate messages to the user
     *
     * @param event the action event that triggered this action, containing module and context information
     */
    override fun actionPerformed(event: AnActionEvent) {
        val module = event.getData(PlatformCoreDataKeys.MODULE) ?: return
        val defaultPagePath = getDefaultElementPath(event, module) ?: return

        val dialog = AddNewComponentDialog(module, defaultPagePath, true)
        val builder = DialogBuilder(module.project)
        builder.setCenterPanel(dialog.contentPane)
        builder.setTitle("New Tapestry Page")
        builder.setPreferredFocusComponent(dialog.nameComponent)

        builder.setOkOperation {
            val pageName = dialog.newComponentName
            if (!Validators.isValidComponentName(pageName)) {
                Messages.showErrorDialog("Invalid page name!", CommonBundle.getErrorTitle())
                return@setOkOperation
            }

            val state = TapestryModuleSupportLoader.getInstance(module).state
            state.newPagesClassesSourceDirectory = dialog.classSourceDirectory.path
            state.newPagesTemplatesSourceDirectory = dialog.templateSourceDirectory.path

            // A command, so the new class and template land as one undoable unit and one Local History entry.
            var failure: String? = null
            WriteCommandAction.writeCommandAction(module.project)
                .withName("New Tapestry Page")
                .run<RuntimeException> {
                    val psiManager = PsiManager.getInstance(module.project)
                    val classSourceDirectory = psiManager.findDirectory(dialog.classSourceDirectory)
                    if (classSourceDirectory != null) {
                        val templateSourceDirectory =
                            if (dialog.isNotCreatingTemplate) null
                            else psiManager.findDirectory(dialog.templateSourceDirectory)
                        try {
                            TapestryUtils.createPage(
                                module, classSourceDirectory, templateSourceDirectory, pageName,
                                dialog.isReplaceExistingFiles
                            )
                        } catch (ex: IllegalStateException) {
                            failure = ex.message
                        }
                    }
                }

            // Reported after the write action: showing a modal dialog while holding the write lock is not allowed.
            failure?.let { Messages.showWarningDialog(module.project, it, "Error Creating Page") }
            builder.window.dispose()
        }

        builder.showModal(true)
    }
}
