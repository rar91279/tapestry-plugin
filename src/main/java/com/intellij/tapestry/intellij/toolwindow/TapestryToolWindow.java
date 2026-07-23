package com.intellij.tapestry.intellij.toolwindow;

import com.intellij.ProjectTopics;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.tapestry.core.TapestryProject;
import com.intellij.tapestry.core.events.FileSystemListenerAdapter;
import com.intellij.tapestry.core.java.IJavaClassType;
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;
import com.intellij.tapestry.core.resource.IResource;
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader;
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType;
import com.intellij.tapestry.intellij.util.IdeaUtils;
import com.intellij.tapestry.intellij.util.TapestryUtils;
import com.intellij.tapestry.lang.TmlFileType;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class TapestryToolWindow extends FileSystemListenerAdapter {

    private static final String SELECTED_TAB_KEY = "tapestry.toolwindow.selectedTab";

    private JPanel              _mainPanel;
    private JTabbedPane         _tabbedPane;
    private final DocumentationTab    _documentationTab;
   private final DependenciesTab _dependenciesTab;

    private final List<IJavaClassType> _updateOnChangeFiles = new ArrayList<>();
    private Module _module;
    private Object _element;
    private final Project _project;

    public TapestryToolWindow(final Project project) {
        _project = project;
        _tabbedPane.removeAll();

        _documentationTab = new DocumentationTab(_project);
        _tabbedPane.addTab("Live Documentation", _documentationTab.getMainPanel());

        _dependenciesTab = new DependenciesTab();
       _tabbedPane.addTab("Dependencies", _dependenciesTab.getMainPanel());

        // Keep the Dependencies tab in sync with whatever element the doc browser shows.
        _documentationTab.setElementListener(element -> _dependenciesTab.showDependencies(null, element));

        // Restore the last-used tab, defaulting to Live Documentation the first time.
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        int savedTab = properties.getInt(SELECTED_TAB_KEY, 0);
        _tabbedPane.setSelectedIndex(savedTab >= 0 && savedTab < _tabbedPane.getTabCount() ? savedTab : 0);
        _tabbedPane.addChangeListener(e -> properties.setValue(SELECTED_TAB_KEY, _tabbedPane.getSelectedIndex(), 0));

        ModuleListener moduleListener = new ModuleListener() {
            @Override
            public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
                reload();
            }

            @Override
            public void moduleAdded(@NotNull Project project, @NotNull Module module) {
                reload();
            }
        };

        MessageBusConnection messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.<ModuleListener>subscribe(ProjectTopics.MODULES, moduleListener);

        // Follow the active editor: show the Tapestry element of the selected tab.
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                syncWithEditor(event.getNewFile());
            }
        });

        reload();
    }

    /**
     * Resolves the Tapestry element (page/component/mixin) of the given editor file and, if any,
     * shows it in the Dependencies tab only — Live Documentation keeps whatever the user is browsing.
     * Resolution runs off the EDT.
     */
    private void syncWithEditor(VirtualFile file) {
        if (file == null) {
            return;
        }

        // All of this touches the index/PSI, so resolve off the EDT.
        ReadAction.nonBlocking(() -> {
                    Module module = ModuleUtilCore.findModuleForFile(file, _project);
                    if (module == null || !TapestryUtils.isTapestryModule(module)) {
                        return null;
                    }
                    return resolveElement(module, file);
                })
                .coalesceBy(this, file)
                .finishOnUiThread(ModalityState.any(), element -> {
                    if (element != null) {
                        _dependenciesTab.showDependencies(null, element);
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private PresentationLibraryElement resolveElement(Module module, VirtualFile file) {
        TapestryProject tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module);
        if (tapestryProject == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(_project).findFile(file);
        if (psiFile == null) {
            return null;
        }
        try {
            if (psiFile instanceof PsiClassOwner) {
                PsiClass psiClass = IdeaUtils.findPublicClass(psiFile);
                if (psiClass == null) {
                    return null;
                }
                return PresentationLibraryElement.createProjectElementInstance(
                        new IntellijJavaClassType(module, psiClass.getContainingFile()), tapestryProject);
            }
            if (psiFile.getFileType().equals(TmlFileType.INSTANCE)) {
                return tapestryProject.findElementByTemplate(psiFile);
            }
        } catch (Exception ex) {
            // Not a Tapestry element — leave the current view untouched.
        }
        return null;
    }


    @Override
    public void fileDeleted(String path) {
        if (_element == null || _module == null) {
            return;
        }

        _documentationTab.showDocumentation(_element, _project);
        _documentationTab.setElement(_element);
    }

    @Override
    public void fileContentsChanged(IResource changedFile) {
        if (_element == null || _module == null) {
            return;
        }

        for (IJavaClassType classType : _updateOnChangeFiles) {
            IResource resource = classType.getFile();

            if (resource == null) {
                continue;
            }

            if (resource.getFile() != null && resource.getFile().getAbsolutePath().endsWith(changedFile.getFile().getAbsolutePath())) {
                _documentationTab.showDocumentation(_element, _project);
                _documentationTab.setElement(_element);
            }
        }
    }

    /**
     * Updates the toolwindow state.
     *
     * @param module              the module the element belongs to.
     * @param element             the element to update for.
     * @param updateOnChangeFiles the list of files to update
     */
    public void update(Module module, Object element, final List<IJavaClassType> updateOnChangeFiles) {
        _module = module;
        _element = element;

        _documentationTab.showDocumentation(element, _project);
        _documentationTab.setElement(_element);

        if (element != null) {
            _updateOnChangeFiles.clear();
            _updateOnChangeFiles.addAll(updateOnChangeFiles);
        }
    }

    public JPanel getMainPanel() {
        return _mainPanel;
    }

    public DocumentationTab getDocumentationTab() {
        return _documentationTab;
    }

    public DependenciesTab getDependenciesTab() {
        return _dependenciesTab;
    }

    private void reload() {
        for (Module module : ModuleManager.getInstance(_project).getModules()) {
            TapestryModuleSupportLoader.getTapestryProject(module).getEventsManager().removeFileSystemListener(this);

            TapestryModuleSupportLoader.getTapestryProject(module).getEventsManager().addFileSystemListener(this);
        }
    }
}
