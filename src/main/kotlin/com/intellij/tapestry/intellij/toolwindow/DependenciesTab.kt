package com.intellij.tapestry.intellij.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.util.EditSourceUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.tapestry.core.model.presentation.InjectedElement
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.tapestry.core.resource.IResource
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType
import com.intellij.tapestry.intellij.core.java.IntellijJavaField
import com.intellij.tapestry.intellij.core.resource.IntellijResource
import com.intellij.tapestry.intellij.toolwindow.nodes.*
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.JPanel
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class DependenciesTab {

    private val dependenciesTree = Tree().apply {
        cellRenderer = DependenciesTreeCellRenderer()
        showsRootHandles = true
        isVisible = false
    }
    // FlowLayout keeps the action buttons at their natural size, packed left (a JToolBar's BoxLayout
    // stretches the unbounded-max-width ActionButtons across the whole bar instead).
    private val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
    val mainPanel: JComponent = BorderLayoutPanel().apply {
        addToTop(toolbar)
        addToCenter(JBScrollPane(dependenciesTree))
    }

    private val presentations = PresentationFactory()
    private val navigateToElementAction = NavigateToElementAction()
    private val navigateToUsageAction = NavigateToUsageAction()

    init {
        dependenciesTree.addMouseListener(object : PopupHandler() {
            override fun invokePopup(comp: Component, x: Int, y: Int) {
                val selected = dependenciesTree.selectionPath
                if (selected != null) {
                    val selectedObject = (selected.lastPathComponent as DefaultMutableTreeNode).userObject
                    if (selectedObject is InjectedElement || selectedObject is PresentationLibraryElement || selectedObject is IResource) {
                        val actions = DefaultActionGroup.createPopupGroup { "NavigateToGroup" }
                        actions.add(navigateToElementAction)
                        actions.add(navigateToUsageAction)
                        actions.addSeparator()
                        actions.add(collapseAllAction())
                        actions.add(expandAllAction())
                        ActionManager.getInstance().createActionPopupMenu("ElementUsagesTree", actions)
                            .component.show(comp, x, y)
                    }
                } else {
                    val actions = DefaultActionGroup.createPopupGroup { "NavigateToGroup" }
                    actions.add(collapseAllAction())
                    actions.add(expandAllAction())
                    ActionManager.getInstance().createActionPopupMenu("ElementUsagesTree", actions)
                        .component.show(comp, x, y)
                }
            }
        })

        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                val selected = dependenciesTree.selectionPath ?: return false
                val selectedObject = (selected.lastPathComponent as DefaultMutableTreeNode).userObject
                // Embedded component / injected page leaves navigate to the element class.
                if (selectedObject is InjectedElement) {
                    navigate((selectedObject.element.elementClass as IntellijJavaClassType).psiClass)
                }
                // "Used By" leaves navigate to the referencing element's class.
                if (selectedObject is PresentationLibraryElement) {
                    navigate((selectedObject.elementClass as IntellijJavaClassType).psiClass)
                }
                // Template / message-catalog resource leaves navigate to their file.
                if (selectedObject is IntellijResource) {
                    navigate(selectedObject.psiFile)
                }
                return true
            }
        }.installOn(dependenciesTree)

        dependenciesTree.addTreeSelectionListener { updateNavigationActions() }

        updateNavigationActions()

        val collapseAction = collapseAllAction()
        val expandAction = expandAllAction()

        toolbar.add(toolbarButton(navigateToElementAction, "Navigate to Element"))
        toolbar.add(toolbarButton(navigateToUsageAction, "Navigate to Usage"))
        toolbar.add(Box.createHorizontalStrut(8))
        toolbar.add(toolbarButton(expandAction, "Expand All"))
        toolbar.add(toolbarButton(collapseAction, "Collapse All"))
    }

    private fun toolbarButton(action: AnAction, tooltip: String): ActionButton {
        val button = ActionButton(action, presentations.getPresentation(action), tooltip, Dimension(24, 24))
        button.toolTipText = tooltip
        return button
    }

    /** Shows the dependencies of an element (a [PresentationLibraryElement]); otherwise clears the tree. */
    fun showDependencies(module: Module?, element: Any?) {
        if (element !is PresentationLibraryElement) {
            clear()
            return
        }
        // Building the tree resolves PSI/annotations — do it off the EDT, then install the model.
        ReadAction.nonBlocking<DependenciesRootNode> { DependenciesRootNode(element) }
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { root ->
                dependenciesTree.isVisible = true
                dependenciesTree.model = DefaultTreeModel(root)
                updateNavigationActions()
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    /** Clear the dependencies tree. */
    fun clear() {
        dependenciesTree.isVisible = false
        updateNavigationActions()
    }

    private fun expandAllAction(): AnAction =
        CommonActionsManager.getInstance().createExpandAllAction(DefaultTreeExpander(dependenciesTree), dependenciesTree)

    private fun collapseAllAction(): AnAction =
        CommonActionsManager.getInstance().createCollapseAllAction(DefaultTreeExpander(dependenciesTree), dependenciesTree)

    private fun updateNavigationActions() {
        var canNavigateToElement = false
        var canNavigateToUsage = false

        val selected = dependenciesTree.selectionPath
        if (selected != null) {
            val node = selected.lastPathComponent as DefaultMutableTreeNode
            val userObject = node.userObject
            canNavigateToElement = userObject is PresentationLibraryElement
                    || userObject is InjectedElement
                    || userObject is IntellijResource
            canNavigateToUsage = node is EmbeddedComponentNode || node is InjectedPageNode
        }

        presentations.getPresentation(navigateToElementAction).isEnabled = canNavigateToElement
        presentations.getPresentation(navigateToUsageAction).isEnabled = canNavigateToUsage
    }

    private inner class NavigateToElementAction :
        AnAction("Navigate to Element", "Navigate to the selected element class", AllIcons.Actions.PreviousOccurence) {
        override fun actionPerformed(event: AnActionEvent) {
            val path = dependenciesTree.selectionPath ?: return
            val selectedObject = (path.lastPathComponent as DefaultMutableTreeNode).userObject
            if (selectedObject is PresentationLibraryElement) {
                navigate((selectedObject.elementClass as IntellijJavaClassType).psiClass)
            }
            if (selectedObject is InjectedElement) {
                navigate((selectedObject.element.elementClass as IntellijJavaClassType).psiClass)
            }
            if (selectedObject is IntellijResource) {
                navigate(selectedObject.psiFile)
            }
        }
    }

    private inner class NavigateToUsageAction :
        AnAction("Navigate to Usage", "Navigate to part of code where the selected element is used", AllIcons.Actions.Find) {
        override fun actionPerformed(event: AnActionEvent) {
            val path = dependenciesTree.selectionPath ?: return
            val selectedNode = path.lastPathComponent as DefaultMutableTreeNode
            val selectedObject = selectedNode.userObject
            if (selectedObject is PresentationLibraryElement || selectedObject is InjectedElement) {
                var field: PsiField? = null
                var file: PsiFile? = null

                if (selectedNode is EmbeddedComponentNode) {
                    val elementField = selectedNode.injectedComponent.field
                    if (elementField != null) field = (elementField as IntellijJavaField).psiField
                    else file = ((selectedNode.parent as EmbeddedTemplateNode).userObject as IntellijResource).psiFile
                }

                if (selectedNode is InjectedPageNode) {
                    val elementField = selectedNode.injectedPage.field
                    if (elementField != null) field = (elementField as IntellijJavaField).psiField
                    else file = ((selectedNode.parent as EmbeddedTemplateNode).userObject as IntellijResource).psiFile
                }

                navigate(field)
                navigate(file)
            }
        }
    }

    /**
     * Navigates to a PSI target. Resolving the navigation descriptor of a compiled (library) class
     * touches the file index — forbidden on the EDT — so compute it in a background read action,
     * then open it on the UI thread.
     */
    private fun navigate(target: PsiElement?) {
        if (target == null) return
        ReadAction.nonBlocking<com.intellij.pom.Navigatable?> { EditSourceUtil.getDescriptor(target) }
            .finishOnUiThread(ModalityState.any()) { descriptor ->
                if (descriptor != null && descriptor.canNavigate()) descriptor.navigate(true)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}
