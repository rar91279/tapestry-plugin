package com.intellij.tapestry.intellij.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiElement;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.tapestry.core.java.IJavaField;
import com.intellij.tapestry.core.model.presentation.InjectedElement;
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;
import com.intellij.tapestry.core.resource.IResource;
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType;
import com.intellij.tapestry.intellij.core.java.IntellijJavaField;
import com.intellij.tapestry.intellij.core.resource.IntellijResource;
import com.intellij.tapestry.intellij.toolwindow.nodes.*;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.actions.CollapseAllAction;
import com.intellij.ui.treeStructure.actions.ExpandAllAction;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;

public class DependenciesTab {

    private JPanel _mainPanel;
    private JTree _dependenciesTree;
    private JToolBar _toolbar;
    private final NavigateToElementAction _navigateToElementAction;
    private final NavigateToUsageAction _navigateToUsageAction;
    private final PresentationFactory myPresentations = new PresentationFactory();

    public DependenciesTab() {
        _dependenciesTree.setCellRenderer(new DependenciesTreeCellRenderer());

        _navigateToElementAction = new NavigateToElementAction();
        _navigateToUsageAction = new NavigateToUsageAction();

      _dependenciesTree.addMouseListener(new PopupHandler() {
        @Override
        public void invokePopup(Component comp, int x, int y) {
          TreePath selected = _dependenciesTree.getSelectionPath();

          // When object it's selected
          if (selected != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) _dependenciesTree.getSelectionPath().getLastPathComponent();
            Object selectedObject = selectedNode.getUserObject();

            if (selectedObject instanceof InjectedElement || selectedObject instanceof PresentationLibraryElement || selectedObject instanceof IResource) {
              DefaultActionGroup actions = DefaultActionGroup.createPopupGroup(() -> "NavigateToGroup");

              actions.add(_navigateToElementAction);
              actions.add(_navigateToUsageAction);

              actions.addSeparator();

              actions.add(new CollapseAllAction(_dependenciesTree));
              actions.add(new ExpandAllAction(_dependenciesTree));

              ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu("ElementUsagesTree", actions);
              popupMenu.getComponent().show(comp, x, y);
            }
          }

          // When object it's not selected
          if (selected == null) {
            DefaultActionGroup actions = DefaultActionGroup.createPopupGroup(() -> "NavigateToGroup");

            actions.add(new CollapseAllAction(_dependenciesTree));
            actions.add(new ExpandAllAction(_dependenciesTree));

            ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu("ElementUsagesTree", actions);
            popupMenu.getComponent().show(comp, x, y);
          }
        }
      });

      new DoubleClickListener() {
        @Override
        protected boolean onDoubleClick(@NotNull MouseEvent e) {
          TreePath selected = _dependenciesTree.getSelectionPath();

          // When is double click
          if (selected != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) _dependenciesTree.getSelectionPath().getLastPathComponent();
            Object selectedObject = selectedNode.getUserObject();

            // Embedded component / injected page leaves navigate to the element class.
            if (selectedObject instanceof InjectedElement) {
              navigate(((IntellijJavaClassType) ((InjectedElement) selectedObject).getElement().getElementClass()).getPsiClass());
            }

            // Template / message-catalog resource leaves navigate to their file.
            if (selectedObject instanceof IntellijResource) {
              navigate(((IntellijResource) selectedObject).getPsiFile());
            }
            return true;
          }
          return false;
        }
      }.installOn(_dependenciesTree);


        _dependenciesTree.addTreeSelectionListener(
                new TreeSelectionListener() {
                    @Override
                    public void valueChanged(TreeSelectionEvent event) {
                        updateNavigationActions();
                    }
                }
        );
        _dependenciesTree.setVisible(false);

        updateNavigationActions();

        CollapseAllAction collapseAllAction = new CollapseAllAction(_dependenciesTree);
        ExpandAllAction expandAllAction = new ExpandAllAction(_dependenciesTree);

        ActionButton navigateToElement = new ActionButton(_navigateToElementAction, myPresentations.getPresentation(_navigateToElementAction), "Navigate to Element", new Dimension(24, 24));
        navigateToElement.setToolTipText("Navigate to Element");
        _toolbar.add(navigateToElement);

        ActionButton navigateToUsage = new ActionButton(_navigateToUsageAction, myPresentations.getPresentation(_navigateToUsageAction), "Navigate to Usage", new Dimension(24, 24));
        navigateToUsage.setToolTipText("Navigate to Usage");
        _toolbar.add(navigateToUsage);
        _toolbar.addSeparator();

        ActionButton expandAll = new ActionButton(expandAllAction, myPresentations.getPresentation(expandAllAction), expandAllAction.getTemplatePresentation().getText(), new Dimension(24, 24));
        expandAll.setToolTipText("Expand All");
        _toolbar.add(expandAll);

        ActionButton collapseAll = new ActionButton(collapseAllAction, myPresentations.getPresentation(collapseAllAction), collapseAllAction.getTemplatePresentation().getText(), new Dimension(24, 24));
        collapseAll.setToolTipText("Collapse All");
        _toolbar.add(collapseAll);

    }

    public JPanel getMainPanel() {
        return _mainPanel;
    }

    /**
     * Shows the dependencies of an element.
     *
     * @param module  the module the element belongs to.
     * @param element the element to show the dependencies of.
     */
    public void showDependencies(Module module, Object element) {
        if (!shouldShowDependencies(element)) {
            clear();
            return;
        }

        // Building the tree resolves PSI/annotations — do it off the EDT, then install the model.
        ReadAction.nonBlocking(() -> new DependenciesRootNode(element))
                .coalesceBy(this)
                .finishOnUiThread(ModalityState.any(), root -> {
                    _dependenciesTree.setVisible(true);
                    _dependenciesTree.setModel(new DefaultTreeModel(root));
                    updateNavigationActions();
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    /**
     * Clear the dependencies tree.
     */
    public void clear() {
        _dependenciesTree.setVisible(false);
        updateNavigationActions();
    }

    private boolean shouldShowDependencies(Object element) {
        return element instanceof PresentationLibraryElement;
    }

    /** Enables the toolbar/popup navigation actions based on the current tree selection. */
    private void updateNavigationActions() {
        boolean canNavigateToElement = false;
        boolean canNavigateToUsage = false;

        TreePath selected = _dependenciesTree.getSelectionPath();
        if (selected != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selected.getLastPathComponent();
            Object userObject = node.getUserObject();
            canNavigateToElement = userObject instanceof PresentationLibraryElement
                    || userObject instanceof InjectedElement
                    || userObject instanceof IntellijResource;
            canNavigateToUsage = node instanceof EmbeddedComponentNode || node instanceof InjectedPageNode;
        }

        myPresentations.getPresentation(_navigateToElementAction).setEnabled(canNavigateToElement);
        myPresentations.getPresentation(_navigateToUsageAction).setEnabled(canNavigateToUsage);
    }

    public class NavigateToElementAction extends AnAction {

        public NavigateToElementAction() {
            super("Navigate to Element", "Navigate to the selected element class", AllIcons.Actions.PreviousOccurence);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) _dependenciesTree.getSelectionPath().getLastPathComponent();
            Object selectedObject = selectedNode.getUserObject();

            if (selectedObject instanceof PresentationLibraryElement) {
                navigate(((IntellijJavaClassType) ((PresentationLibraryElement) selectedObject).getElementClass()).getPsiClass());
            }
            if (selectedObject instanceof InjectedElement) {
                navigate(((IntellijJavaClassType) ((InjectedElement) selectedObject).getElement().getElementClass()).getPsiClass());
            }
            if (selectedObject instanceof IntellijResource) {
                navigate(((IntellijResource) selectedObject).getPsiFile());
            }
        }
    }

    private class NavigateToUsageAction extends AnAction {

        NavigateToUsageAction() {
            super("Navigate to Usage", "Navigate to part of code where the selected element is used", AllIcons.Actions.Find);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) _dependenciesTree.getSelectionPath().getLastPathComponent();
            Object selectedObject = selectedNode.getUserObject();

            if (selectedObject instanceof PresentationLibraryElement || selectedObject instanceof InjectedElement) {
                PsiField field = null;
                PsiFile file = null;

                // Embedded component
                if (selectedNode instanceof EmbeddedComponentNode) {
                    IJavaField elementField = ((EmbeddedComponentNode) selectedNode).getInjectedComponent().getField();

                    if (elementField != null)
                        field = ((IntellijJavaField) elementField).getPsiField();
                    else
                        file = ((IntellijResource) ((EmbeddedTemplateNode) selectedNode.getParent()).getUserObject()).getPsiFile();
                }

                // Injected page
                if (selectedNode instanceof InjectedPageNode) {
                    IJavaField elementField = ((InjectedPageNode) selectedNode).getInjectedPage().getField();

                    if (elementField != null)
                        field = ((IntellijJavaField) elementField).getPsiField();
                    else
                        file = ((IntellijResource) ((EmbeddedTemplateNode) selectedNode.getParent()).getUserObject()).getPsiFile();
                }

                navigate(field);
                navigate(file);
            }
        }
    }

    /**
     * Navigates to a PSI target. Resolving the navigation descriptor of a compiled (library)
     * class touches the file index — a slow operation forbidden on the EDT — so compute it in a
     * background read action, then open it on the UI thread.
     */
    private static void navigate(PsiElement target) {
        if (target == null) {
            return;
        }
        ReadAction.nonBlocking(() -> EditSourceUtil.getDescriptor(target))
                .finishOnUiThread(ModalityState.any(), descriptor -> {
                    if (descriptor != null && descriptor.canNavigate()) {
                        descriptor.navigate(true);
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }
}
