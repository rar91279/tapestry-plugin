package com.intellij.tapestry.intellij.toolwindow

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.tapestry.core.events.FileSystemListenerAdapter
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.tapestry.core.resource.IResource
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType
import com.intellij.tapestry.intellij.util.IdeaUtils
import com.intellij.tapestry.intellij.util.TapestryUtils
import com.intellij.tapestry.lang.TmlFileType
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class TapestryToolWindow(private val project: Project) : FileSystemListenerAdapter() {

    private val tabbedPane = JBTabbedPane(JBTabbedPane.BOTTOM)
    val mainPanel: JComponent = JPanel(BorderLayout()).apply { add(tabbedPane, BorderLayout.CENTER) }

    val documentationTab = DocumentationTab(project)
    val dependenciesTab = DependenciesTab()

    private val updateOnChangeFiles = mutableListOf<IJavaClassType>()
    private var module: Module? = null
    private var element: Any? = null

    init {
        tabbedPane.addTab("Live Documentation", documentationTab.mainPanel)
        tabbedPane.addTab("Dependencies", dependenciesTab.mainPanel)

        // Keep the Dependencies tab in sync with whatever element the doc browser shows.
        documentationTab.setElementListener { element -> dependenciesTab.showDependencies(element) }

        // Restore the last-used tab, defaulting to Live Documentation the first time.
        val properties = PropertiesComponent.getInstance(project)
        val savedTab = properties.getInt(SELECTED_TAB_KEY, 0)
        val targetTab = if (savedTab in 0 until tabbedPane.tabCount) savedTab else 0
        tabbedPane.addChangeListener { properties.setValue(SELECTED_TAB_KEY, tabbedPane.selectedIndex, 0) }
        // Defer to after the tool window is realized and force a real selection change: selecting the
        // already-current index is a no-op, so the heavyweight JCEF browser never paints until first click.
        SwingUtilities.invokeLater {
            tabbedPane.selectedIndex = -1
            tabbedPane.selectedIndex = targetTab
        }

        val moduleListener = object : ModuleListener {
            override fun moduleRemoved(project: Project, module: Module) = reload()
            override fun modulesAdded(project: Project, modules: List<Module>) = reload()
        }

        val messageBusConnection = project.messageBus.connect()
        messageBusConnection.subscribe(ModuleListener.TOPIC, moduleListener)

        // Follow the active editor: show the Tapestry element of the selected tab.
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) = syncWithEditor(event.newFile)
        })

        reload()
    }

    /**
     * Resolves the Tapestry element (page/component/mixin) of the given editor file and, if any,
     * shows it in the Dependencies tab only — Live Documentation keeps whatever the user is browsing.
     * Resolution runs off the EDT.
     */
    private fun syncWithEditor(file: VirtualFile?) {
        if (file == null) return

        // All of this touches the index/PSI, so resolve off the EDT.
        ReadAction.nonBlocking<PresentationLibraryElement?> {
            val module = ModuleUtilCore.findModuleForFile(file, project)
            if (module == null || !TapestryUtils.isTapestryModule(module)) null
            else resolveElement(module, file)
        }
            .coalesceBy(this, file)
            .finishOnUiThread(ModalityState.any()) { element ->
                if (element != null) dependenciesTab.showDependencies(element)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun resolveElement(module: Module, file: VirtualFile): PresentationLibraryElement? {
        val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module) ?: return null
        val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file) ?: return null
        try {
            if (psiFile is PsiClassOwner) {
                val psiClass = IdeaUtils.findPublicClass(psiFile) ?: return null
                return PresentationLibraryElement.createProjectElementInstance(
                    IntellijJavaClassType(module, psiClass.containingFile), tapestryProject)
            }
            if (psiFile.fileType == TmlFileType) {
                return tapestryProject.findElementByTemplate(psiFile)
            }
        } catch (_: Exception) {
            // Not a Tapestry element — leave the current view untouched.
        }
        return null
    }

    override fun fileDeleted(path: String?) {
        if (element == null || module == null) return
        documentationTab.showDocumentation(element)
        documentationTab.setElement(element)
    }

    override fun fileContentsChanged(changedFile: IResource) {
        if (element == null || module == null) return
        val changedPath = changedFile.file?.absolutePath ?: return
        for (classType in updateOnChangeFiles) {
            val resourceFile = classType.file?.file ?: continue
            if (resourceFile.absolutePath.endsWith(changedPath)) {
                documentationTab.showDocumentation(element)
                documentationTab.setElement(element)
            }
        }
    }

    /** Updates the toolwindow state for the given module/element. */
    fun update(module: Module?, element: Any?, updateOnChangeFiles: List<IJavaClassType>) {
        this.module = module
        this.element = element

        documentationTab.showDocumentation(element)
        documentationTab.setElement(element)

        if (element != null) {
            this.updateOnChangeFiles.clear()
            this.updateOnChangeFiles.addAll(updateOnChangeFiles)
        }
    }

    private fun reload() {
        for (module in ModuleManager.getInstance(project).modules) {
            val eventsManager = TapestryModuleSupportLoader.getTapestryProject(module)?.eventsManager ?: continue
            eventsManager.removeFileSystemListener(this)
            eventsManager.addFileSystemListener(this)
        }
    }

    companion object {
        private const val SELECTED_TAB_KEY = "tapestry.toolwindow.selectedTab"
    }
}
