package com.github.rar91279.plugin.tapestry.intellij.actions.safedelete

import com.intellij.ide.DeleteProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.RefactoringFactory
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResource
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.view.TapestryProjectViewPane
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.LibrariesNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.PackageNode
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.TapestryNode
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/**
 * Provides safe delete functionality for Tapestry elements in the IntelliJ IDEA plugin.
 *
 * This provider handles deletion of Tapestry-specific elements including:
 * - Individual PSI files
 * - Presentation library elements (component classes with their templates and message catalogs)
 * - Package nodes containing multiple Tapestry elements
 *
 * The provider integrates with IntelliJ's refactoring system to perform safe deletion
 * with usage preview before actual deletion occurs.
 */
class SafeDeleteProvider : DeleteProvider {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    /**
     * Performs the safe delete operation on selected elements from the Tapestry project view.
     *
     * This method collects all PSI elements to be deleted based on the current selection in the
     * Tapestry project view pane. It handles different types of elements:
     * - For PSI files, adds the file directly to the deletion list
     * - For presentation elements, adds the class, all templates, and all message catalogs
     * - For package nodes, recursively collects all presentation elements within the package
     *
     * The collected elements are then passed to IntelliJ's safe delete refactoring with
     * usage preview enabled.
     *
     * @param dataContext the action data context containing the current project and selection information
     */
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

    /**
     * Determines whether the selected elements can be safely deleted.
     *
     * This method validates that all selected elements meet the criteria for deletion:
     * - Presentation elements must belong to the application library (not external libraries)
     * - PSI files must not be in the local file system (e.g., TML files)
     * - Package nodes must not be under the Libraries node
     *
     * @param dataContext the action data context containing the current project and selection information
     * @return true if all selected elements can be deleted, false otherwise
     */
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

            // The element to delete is a TML file (the previous check was "not a local VFS impl file")
            if (element is PsiFile && element.virtualFile?.isInLocalFileSystem == false) {
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

    /**
     * Adds the class, templates and message catalogs of the node's presentation element to [elementsList].
     *
     * @param child the tree node containing the presentation element to be deleted
     * @param elementsList the mutable list to which PSI elements (class, templates, catalogs) will be added
     */
    private fun addElementToDelete(child: DefaultMutableTreeNode, elementsList: MutableList<PsiElement>) {
        val element = (child.userObject as TapestryNode).getValue() as PresentationLibraryElement
        val elementClass = (element.elementClass.file as IntellijResource).psiFile

        elementsList.add(IdeaUtils.findPublicClass(elementClass)!!)

        for (template in element.template) elementsList.add((template as IntellijResource).psiFile)
        for (catalog in element.messageCatalog) elementsList.add((catalog as IntellijResource).psiFile)
    }
}
