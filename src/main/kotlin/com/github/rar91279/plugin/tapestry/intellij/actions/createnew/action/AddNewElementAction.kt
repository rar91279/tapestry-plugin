package com.github.rar91279.plugin.tapestry.intellij.actions.createnew.action

import com.intellij.CommonBundle
import com.intellij.javaee.web.WebUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.util.PathUtils
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.AbstractModuleNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.LibrariesNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.DirectoryNode
import java.io.File
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Abstract base class for actions that add new Tapestry elements (pages, components, mixins) to a project.
 *
 * This action provides common functionality for creating new Tapestry elements from either the project view
 * or the Tapestry-specific view. It handles:
 * - Validation that the module is a Tapestry module
 * - Determining the appropriate package/directory context for the new element
 * - Enabling/disabling the action based on the current selection
 * - Deriving default paths for new elements based on the selected package or directory
 *
 * Subclasses must implement [getElementsRootPackage] to specify where their particular element type
 * (pages, components, or mixins) should be created.
 *
 * @param T the type of [DirectoryNode] this action works with (e.g., PagesNode, ComponentsNode, MixinsNode)
 * @param nodeClass the class object for the node type, used to identify valid selection contexts
 */
abstract class AddNewElementAction<T : DirectoryNode>(private val nodeClass: Class<T>) : AnAction() {

    /**
     * Updates the presentation state of this action based on the current context.
     *
     * The action is enabled when:
     * - The current module is a valid Tapestry module
     * - The selection is within or ancestral to the element's root package, OR
     * - The selection is within a web root directory (for template creation), OR
     * - The selection is a Tapestry view node of the appropriate type (not inside Libraries)
     *
     * @param event the action event containing information about the current context
     */
    override fun update(event: AnActionEvent) {
        var enabled = false
        val presentation = event.presentation
        val module = event.getData(PlatformCoreDataKeys.MODULE)

        if (!TapestryUtils.isTapestryModule(module)) {
            presentation.isEnabledAndVisible = false
            return
        }

        val data = event.getData(PlatformCoreDataKeys.SELECTED_ITEM)
        val element = data as? DefaultMutableTreeNode
        // it's the project view
        if (element == null) {
            val eventPsiElement = event.getData(CommonDataKeys.PSI_ELEMENT)
            val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module)
            if (tapestryProject == null) {
                presentation.isEnabledAndVisible = false
                return
            }
            val aPackage = getElementsRootPackage(tapestryProject)
            if (aPackage == null) {
                presentation.isEnabledAndVisible = false
                return
            }

            val eventPackage = IdeaUtils.getPackage(eventPsiElement)
            if (event.getData(LangDataKeys.MODULE_CONTEXT) != null) {
                // The module node itself is selected — create at the element root package.
                enabled = true
            } else if (eventPackage != null) {
                val elementsRootPackage = JavaPsiFacade.getInstance(module!!.project).findPackage(aPackage)
                // Enable inside the element package *or* on any ancestor of it (module/app-root package),
                // so the action isn't hidden unless you drill all the way into pages/components/mixins.
                if (elementsRootPackage != null &&
                    (eventPackage.qualifiedName.startsWith(elementsRootPackage.qualifiedName) ||
                        elementsRootPackage.qualifiedName.startsWith(eventPackage.qualifiedName))) {
                    enabled = true
                }
            } else {
                if (JavaPsiFacade.getInstance(module!!.project).findPackage(aPackage) == null) {
                    presentation.isEnabled = false
                    return
                }
                val webFacet = IdeaUtils.getWebFacet(module)
                if (eventPsiElement is PsiDirectory && webFacet != null &&
                    WebUtil.isInsideWebRoots(eventPsiElement.virtualFile, webFacet.webRoots)) {
                    enabled = true
                }
            }
        }
        // it's the Tapestry view | the module itself. An empty category isn't shown, so the module node is
        // where the first page, component or mixin of a module gets created — at the element root package.
        else if (element.userObject is AbstractModuleNode) {
            enabled = true
        }
        // it's the Tapestry view | folder
        else if (element.userObject is DirectoryNode) {
            val session = event.updateSession
            if (session.compute(this, "findParent", ActionUpdateThread.EDT) {
                    (IdeaUtils.findFirstParent(element, nodeClass) != null || nodeClass.isInstance(element.userObject)) &&
                        IdeaUtils.findFirstParent(element, LibrariesNode::class.java) == null
                }) {
                enabled = true
            }
        }
        presentation.isVisible = true
        presentation.isEnabled = enabled
    }

    /**
     * Specifies that this action's update method should be called on a background thread.
     *
     * @return [ActionUpdateThread.BGT] to indicate background thread execution
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    protected abstract fun getElementsRootPackage(tapestryProject: TapestryProject): String?

    /**
     * Derives the default path for a new element based on the current selection context.
     *
     * The path is determined by:
     * - If a package is selected: the relative path from the element's root package
     * - If a web root directory is selected: the relative path from that web root
     * - Otherwise: an empty path (element will be created at the root level)
     *
     * @param event the action event containing information about the current selection
     * @param module the module in which to create the element
     * @return the default path for the new element (may be empty string), or null if an error occurs
     */
    protected fun getDefaultElementPath(event: AnActionEvent, module: Module): String? {
        val eventPsiElement = event.getData(CommonDataKeys.PSI_ELEMENT)
        val psiPackage = IdeaUtils.getPackage(eventPsiElement)
        var defaultPagePath = ""
        if (psiPackage != null) {
            val eventPackage = psiPackage.qualifiedName
            val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module)
            if (tapestryProject == null) {
                showError()
                return null
            }
            val basePagesPackage = getElementsRootPackage(tapestryProject)
            if (basePagesPackage == null) {
                showError()
                return null
            }
            try {
                defaultPagePath = PathUtils.packageIntoPath(eventPackage.substring(basePagesPackage.length + 1), true)
            } catch (_: StringIndexOutOfBoundsException) {
                // ignore
            }
        }

        // No package context (e.g., a web-root directory, or the module node itself): derive the path
        // from the enclosing web root when there is one, otherwise fall back to the element root (").
        if (eventPsiElement is PsiDirectory && psiPackage == null) {
            val webFacet = IdeaUtils.getWebFacet(module)
            val webRoot = if (webFacet != null) WebUtil.findParentWebRoot(eventPsiElement.virtualFile, webFacet.webRoots) else null
            if (webRoot?.file != null) {
                defaultPagePath = eventPsiElement.virtualFile.path.replaceFirst(webRoot.file!!.path.toRegex(), "") +
                    PathUtils.TAPESTRY_PATH_SEPARATOR
                if (defaultPagePath.startsWith(File.separator)) {
                    defaultPagePath = defaultPagePath.substring(1)
                }
                if (defaultPagePath == PathUtils.TAPESTRY_PATH_SEPARATOR) {
                    defaultPagePath = ""
                }
            }
        }
        return defaultPagePath
    }

    private fun showError() = Messages.showErrorDialog(
            "Can't create element. Please check if this module is a valid Tapestry application!",
            CommonBundle.getErrorTitle())
}
