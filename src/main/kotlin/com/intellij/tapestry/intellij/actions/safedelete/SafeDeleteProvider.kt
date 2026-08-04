package com.intellij.tapestry.intellij.actions.safedelete

import com.intellij.ide.DeleteProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.RefactoringFactory
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.tapestry.intellij.core.resource.IntellijResource
import com.intellij.tapestry.intellij.util.IdeaUtils
import com.intellij.tapestry.intellij.view.TapestryProjectViewPane
import com.intellij.tapestry.intellij.view.nodes.LibrariesNode
import com.intellij.tapestry.intellij.view.nodes.PackageNode
import com.intellij.tapestry.intellij.view.nodes.TapestryNode
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/**
 * Safe Delete action provider.
 */
class SafeDeleteProvider : DeleteProvider {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun deleteElement(dataContext: DataContext) {
        val project = dataContext.getData(CommonDataKeys.PROJECT)!!

        val totalElementsToDelete = ArrayList<PsiElement>()
        for (treePath in TapestryProjectViewPane.getInstance(project).selectionPaths!!) {
            val elementsList = ArrayList<PsiElement>()
            var node: DefaultMutableTreeNode? = treePath.lastPathComponent as DefaultMutableTreeNode
            val element = (node!!.userObject as TapestryNode).getValue()

            // The selected node is a file
            if (element is PsiFile) {
                totalElementsToDelete.add(element)
            }

            // The selected node is a presentation element
            if (element is PresentationLibraryElement) {
                val elementClass = (element.elementClass.file as IntellijResource).psiFile
                totalElementsToDelete.add(elementClass)

                for (template in element.template) totalElementsToDelete.add((template as IntellijResource).psiFile)
                for (catalog in element.messageCatalog) totalElementsToDelete.add((catalog as IntellijResource).psiFile)
            }

            // The selected node is a package
            if (node.userObject is PackageNode) {
                val tree = TapestryProjectViewPane.getInstance(project).tree
                val expanded = tree.isExpanded(TreePath(node.path))
                val starterNode = node

                totalElementsToDelete.add((node.userObject as TapestryNode).getValue() as PsiElement)

                // Exist nodes
                while (node != null &&
                    (node.userObject is PackageNode || (node.userObject as TapestryNode).getValue() is PresentationLibraryElement)) {
                    val numberChildren = (node.userObject as TapestryNode).children.size

                    tree.expandPath(TreePath(node.path))

                    // Search all the children
                    for (i in 0 until numberChildren) {
                        val child = node.getChildAt(i) as DefaultMutableTreeNode
                        // The node is a presentation element
                        if ((child.userObject as TapestryNode).getValue() is PresentationLibraryElement) {
                            addElementToDelete(child, elementsList)
                        }
                    }
                    node = if (numberChildren > 0) node.nextNode else null
                }
                totalElementsToDelete.addAll(elementsList)

                if (!expanded) {
                    tree.collapsePath(TreePath(starterNode.path))
                }
            }
        }

        val safeDeleteRefactoring = RefactoringFactory.getInstance(project)
            .createSafeDelete(PsiUtilCore.toPsiElementArray(totalElementsToDelete))
        safeDeleteRefactoring.isPreviewUsages = true
        safeDeleteRefactoring.run()
    }

    override fun canDeleteElement(dataContext: DataContext): Boolean {
        val project = dataContext.getData(CommonDataKeys.PROJECT)

        if (project == null || TapestryProjectViewPane.getInstance(project).selectionPaths == null) {
            return false
        }

        for (treePath in TapestryProjectViewPane.getInstance(project).selectionPaths!!) {
            val node = treePath.lastPathComponent as DefaultMutableTreeNode
            val element = (node.userObject as TapestryNode).getValue()
            var canDelete = false

            // The element to delete is a presentation element.
            if (element is PresentationLibraryElement &&
                element.library?.id == TapestryProject.APPLICATION_LIBRARY_ID) {
                canDelete = true
            }

            // The element to delete is a TML file
            if (element is PsiFile && element.virtualFile !is VirtualFileImpl) {
                canDelete = true
            }

            // The element to delete is a folder node
            if (node.userObject is PackageNode && IdeaUtils.findFirstParent(node, LibrariesNode::class.java) == null) {
                canDelete = true
            }

            if (!canDelete) {
                return false
            }
        }
        return true
    }

    companion object {
        /** Adds the class, templates and message catalogs of the node's presentation element to [elementsList]. */
        private fun addElementToDelete(child: DefaultMutableTreeNode, elementsList: MutableList<PsiElement>) {
            val element = (child.userObject as TapestryNode).getValue() as PresentationLibraryElement
            val elementClass = (element.elementClass.file as IntellijResource).psiFile

            elementsList.add(IdeaUtils.findPublicClass(elementClass)!!)

            for (template in element.template) elementsList.add((template as IntellijResource).psiFile)
            for (catalog in element.messageCatalog) elementsList.add((catalog as IntellijResource).psiFile)
        }
    }
}
