package com.intellij.tapestry.intellij.actions.createnew

import com.intellij.CommonBundle
import com.intellij.javaee.web.WebUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.impl.Utils
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.util.PathUtils
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.intellij.util.IdeaUtils
import com.intellij.tapestry.intellij.util.TapestryUtils
import com.intellij.tapestry.intellij.view.nodes.LibrariesNode
import com.intellij.tapestry.intellij.view.nodes.PackageNode
import java.io.File
import javax.swing.tree.DefaultMutableTreeNode

abstract class AddNewElementAction<T : PackageNode>(private val nodeClass: Class<T>) : AnAction() {

    override fun update(event: AnActionEvent) {
        var enabled = false
        val presentation = event.presentation
        val module = event.getData(PlatformCoreDataKeys.MODULE)

        if (!TapestryUtils.isTapestryModule(module)) {
            presentation.setEnabledAndVisible(false)
            return
        }

        val data = event.getData(PlatformCoreDataKeys.SELECTED_ITEM)
        val element = data as? DefaultMutableTreeNode
        // it's the project view
        if (element == null) {
            val eventPsiElement = event.getData(CommonDataKeys.PSI_ELEMENT)
            val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module)
            if (tapestryProject == null) {
                presentation.setEnabledAndVisible(false)
                return
            }
            val aPackage = getElementsRootPackage(tapestryProject)
            if (aPackage == null) {
                presentation.setEnabledAndVisible(false)
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
        // it's the Tapestry view | folder
        else if (element.userObject is PackageNode) {
            val session = Utils.getOrCreateUpdateSession(event)
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

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    protected abstract fun getElementsRootPackage(tapestryProject: TapestryProject): String?

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
            } catch (ex: StringIndexOutOfBoundsException) {
                // ignore
            }
        }

        // No package context (e.g. a web-root directory, or the module node itself): derive the path
        // from the enclosing web root when there is one, otherwise fall back to the element root ("").
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

    private fun showError() {
        Messages.showErrorDialog(
            "Can't create element. Please check if this module is a valid Tapestry application!",
            CommonBundle.getErrorTitle())
    }
}
