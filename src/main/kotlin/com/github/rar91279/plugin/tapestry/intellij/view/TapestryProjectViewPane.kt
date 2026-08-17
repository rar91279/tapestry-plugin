package com.github.rar91279.plugin.tapestry.intellij.view

import com.github.rar91279.plugin.tapestry.core.events.FileSystemListener
import com.github.rar91279.plugin.tapestry.core.events.TapestryModelChangeListener
import com.github.rar91279.plugin.tapestry.core.exceptions.NotTapestryElementException
import com.intellij.psi.PsiClass
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.psi.PsiFile
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.tapestryScope
import com.github.rar91279.plugin.tapestry.intellij.actions.safedelete.SafeDeleteProvider
import com.github.rar91279.plugin.tapestry.intellij.toolwindow.TapestryToolWindow
import com.github.rar91279.plugin.tapestry.intellij.toolwindow.getToolWindow
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.intellij.view.actions.GroupElementFilesToggleAction
import com.github.rar91279.plugin.tapestry.intellij.view.actions.ShowLibrariesTogleAction
import com.github.rar91279.plugin.tapestry.intellij.view.nodes.*
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.PsiCopyPasteManager
import com.intellij.ide.SelectInTarget
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.AbstractProjectViewPane
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ui.tree.TreeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import icons.TapestryIcons
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

/**
 * The Tapestry view pane.
 */
class TapestryProjectViewPane(project: Project) :
    AbstractProjectViewPane(project), FileSystemListener, TapestryModelChangeListener, TapestryViewOptions {

    private val ideView = TapestryIdeView(this)
    private lateinit var component: JScrollPane
    private var shown = false
    override var showElementFiles = true
        private set
    override var showLibraries = true
        private set
    private val moduleListener = object : ModuleListener {
        override fun moduleRemoved(project: Project, module: Module) = reload()
        override fun modulesAdded(project: Project, modules: List<Module>) = reload()
    }
    // Parented to this pane, so the connection is released even if the pane is discarded without dispose().
    private val messageBusConnection = project.messageBus.connect(this)
    private var structureTreeModel: StructureTreeModel<TapestryProjectTreeStructure>? = null

    // Owns the events-manager subscriptions. Disposed explicitly from dispose() rather than relying on this
    // pane being torn down via Disposer.dispose(), so the subscriptions are released either way.
    private val subscriptions = Disposer.newDisposable("TapestryProjectViewPane subscriptions")

    /** The in-flight background resolution of the current tree selection; superseded by the next selection. */
    private var selectionJob: Job? = null

    /** The project this pane belongs to; exposed for the other view classes in this package. */
    val project: Project get() = myProject

    init {
        messageBusConnection.subscribe(ModuleListener.TOPIC, moduleListener)
        subscribeToModules()
    }

    /**
     * Subscribes to every module's events manager, parented to this pane so the subscriptions die with it.
     * Safe to re-run when modules change: registration is idempotent, and modules gone from the project
     * take their (now unreachable) events manager with them.
     */
    private fun subscribeToModules() {
        ModuleManager.getInstance(myProject).modules
            .mapNotNull { TapestryModuleSupportLoader.getTapestryProject(it)?.eventsManager }
            .onEach {
                it.addFileSystemListener(this, subscriptions)
                it.addTapestryModelListener(this, subscriptions)
            }
    }

    override fun isInitiallyVisible()= ModuleManager.getInstance(myProject).modules.any { TapestryUtils.isTapestryModule(it) }

    override fun addToolbarActions(defaultActionGroup: DefaultActionGroup) {
        defaultActionGroup.childActionsOrStubs
            .filterNot { it.templatePresentation.text == "Autoscroll to Source" }
            .onEach { defaultActionGroup.remove(it) }

        defaultActionGroup.addAction(object : GroupElementFilesToggleAction() {
            override fun isSelected(e: AnActionEvent) = showElementFiles
            override fun setSelected(e: AnActionEvent, state: Boolean) {
                showElementFiles = state
                updateFromRoot(false)
            }
        }).setAsSecondary(true)

        defaultActionGroup.addAction(object : ShowLibrariesTogleAction() {
            override fun isSelected(e: AnActionEvent) = showLibraries
            override fun setSelected(e: AnActionEvent, state: Boolean) {
                showLibraries = state
                updateFromRoot(false)
            }
        }).setAsSecondary(true)
        defaultActionGroup.add(
            CommonActionsManager.getInstance().createCollapseAllAction(DefaultTreeExpander(tree), tree)
        )
    }

    /** Reloads the view pane. */
    fun reload() {
        modulesChanged()
        updateFromRoot(true)
    }

    override fun getTitle(): String = VIEW_TITLE

    override fun getIcon(): Icon = TapestryIcons.Tapestry_logo_small

    override fun getId(): String = ID

    override fun createComponent(): JComponent {
        initTree()
        return component
    }

    override fun updateFromRoot(restoreExpandedPaths: Boolean): ActionCallback {
        structureTreeModel?.invalidateAsync()
        return ActionCallback.DONE
    }

    override fun select(userObject: Any?, virtualFile: VirtualFile?, requestFocus: Boolean) {
        // do nothing
    }

    override fun getWeight(): Int = 5

    override fun createSelectInTarget(): SelectInTarget = TapestryProjectSelectInTarget(myProject)

    override fun fileCreated(path: String?) {
        updateFromRoot(true)
    }

    override fun fileDeleted(path: String?) {
        updateFromRoot(true)
    }

    override fun classCreated(classFqn: String?) {
        updateFromRoot(true)
    }

    override fun classDeleted(classFqn: String?) {
        updateFromRoot(true)
    }

    override fun fileContentsChanged(changedFile: PsiFile) {
        // do nothing
    }

    override fun modelChanged() = reload()

    override fun dispose() {
        selectionJob?.cancel()
        Disposer.dispose(subscriptions)
        messageBusConnection.disconnect()
        super.dispose()
    }

    /** Check if a file can be selected. */
    fun canSelect(): Boolean = pathToSelect.isNotEmpty()

    private val pathToSelect: List<Any> get() = emptyList()

    override fun uiDataSnapshot(sink: DataSink) {
        super.uiDataSnapshot(sink)

        sink[CommonDataKeys.PROJECT] = myProject

        val selected = getSelectedSimpleNode() as? TapestryNode
        if (selected != null) {
            val value = selected.getValue()
            if ((value is PsiDirectory || value is PsiFile) &&
                IdeaUtils.findFirstParent(getSelectedTreeNode(), ExternalLibraryNode::class.java) == null
            ) {
                sink[LangDataKeys.IDE_VIEW] = ideView
            }
            sink[PlatformCoreDataKeys.MODULE] = selected.module
        }

        // PSI-derived, so provided lazily rather than computed eagerly on the EDT.
        //
        // Every node has to answer this, not just the element nodes: the tree is full of files (templates,
        // catalogs, stylesheets, modules) and of declarations (a service's build method, a stack's class), and
        // opening them is the whole point of showing them. `Navigatable` is what the double-click and
        // Enter handlers installed in initTree() look for; without it a node is simply dead.
        sink.lazy(CommonDataKeys.NAVIGATABLE) {
            NodeNavigation.navigatableOf(getSelectedSimpleNode() as? TapestryNode)
        }

        // Lets *Jump to Source* and the *New >* actions work off the selection too — the latter derive the
        // package to create in from the selected element.
        sink.lazy(CommonDataKeys.PSI_ELEMENT) { NodeNavigation.psiElementOf(getSelectedNodeElement()) }

        sink[PlatformDataKeys.DELETE_ELEMENT_PROVIDER] = SafeDeleteProvider()
        sink[PlatformCoreDataKeys.SELECTED_ITEM] = getSelectedTreeNode()
    }

    /** The module of the currently selected Tapestry node, or `null` if none is selected. */
    fun getSelectedModule(): Module? = (getSelectedSimpleNode() as? TapestryNode)?.module

    private fun getSelectedTreeNode(): DefaultMutableTreeNode? =
        (tree?.selectionPath?.lastPathComponent) as? DefaultMutableTreeNode

    private fun getSelectedSimpleNode(): SimpleNode? = getSelectedTreeNode()?.userObject as? SimpleNode

    fun getSelectedNodeElement(): Any? = (getSelectedSimpleNode() as? TapestryNode)?.getValue()

    private fun initTree() {
        val treeStructure = TapestryProjectTreeStructure(RootNode(myProject, this))
        val structureTreeModel = StructureTreeModel(treeStructure, this)
        this.structureTreeModel = structureTreeModel
        val asyncTreeModel = AsyncTreeModel(structureTreeModel, this)

        myTree = object : ProjectViewTree(asyncTreeModel) {
            override fun toString(): String = "${getTitle()} ${super.toString()}"
        }
        myTreeStructure = treeStructure

        myTree.isRootVisible = false
        myTree.showsRootHandles = true
        TreeUtil.expandRootChildIfOnlyOne(myTree)

        myTree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
        EditSourceOnDoubleClickHandler.install(myTree)
        EditSourceOnEnterKeyHandler.install(myTree)
        TreeUtil.installActions(myTree)

        myTree.transferHandler = ViewTransferHandler(this)
        val mouseListener = ViewMouseListener(this)

        myTree.addMouseListener(mouseListener)
        myTree.addMouseMotionListener(mouseListener)

        addTreeListeners()

        TreeSpeedSearch.installOn(myTree)

        component = ScrollPaneFactory.createScrollPane(myTree)
        component.border = BorderFactory.createEmptyBorder()
        CustomizationUtil.installPopupHandler(myTree, IdeActions.GROUP_PROJECT_VIEW_POPUP, "TapestryProjectViewPopup")
    }

    private fun addTreeListeners() {
        tree.selectionModel.addTreeSelectionListener { event ->
            val newPath = event.newLeadSelectionPath ?: return@addTreeSelectionListener
            val toolWindow = getToolWindow(myProject) ?: return@addTreeSelectionListener

            val selectedNode = IdeaUtils.tapestryNodeOf(newPath)

            when {
                selectedNode !is TapestryNode -> toolWindow.update(null, null, emptyList())

                selectedNode is PageNode || selectedNode is ComponentNode || selectedNode is MixinNode -> {
                    val selectedValue = selectedNode.getValue() as PresentationLibraryElement
                    toolWindow.update(getSelectedModule(), selectedValue, listOfNotNull(selectedValue.elementClass))
                }

                selectedNode is ClassNode || selectedNode is FileNode -> {
                    val parentSelectedValue = (IdeaUtils.nodeOf(newPath)?.parent as? DefaultMutableTreeNode)
                        ?.let { it.userObject as? TapestryNode }
                        ?.getValue()

                    if (parentSelectedValue is PresentationLibraryElement) {
                        // Already resolved by the parent node — nothing to look up.
                        toolWindow.update(getSelectedModule(), parentSelectedValue, listOfNotNull(parentSelectedValue.elementClass))
                    } else {
                        resolveSelectionInBackground(selectedNode, toolWindow)
                    }
                }

                else -> toolWindow.update(null, null, emptyList())
            }
        }
        tree.addKeyListener(PsiCopyPasteManager.EscapeHandler())
    }

    /**
     * Resolving a class/template node to its Tapestry element goes through the stub indexes
     * (`createProjectElementInstance` → `TapestryProject.libraries`, `findElementByTemplate`), which must not
     * run on the EDT — a Swing selection listener doing index work freezes the IDE on every arrow-key press
     * and trips the "Slow operations are prohibited on EDT" assertion.
     *
     * `smartReadAction` also means a selection made while indexing waits for smart mode instead of resolving
     * to nothing. Each new selection cancels the previous lookup.
     */
    private fun resolveSelectionInBackground(selectedNode: TapestryNode, toolWindow: TapestryToolWindow) {
        val module = selectedNode.module
        val psiFile = selectedNode.getValue() as? PsiFile ?: return
        val isClassNode = selectedNode is ClassNode
        val selectedModule = getSelectedModule()

        selectionJob?.cancel()
        selectionJob = myProject.tapestryScope.launch {
            val component = smartReadAction(myProject) { resolveElement(module, psiFile, isClassNode) } ?: return@launch

            withContext(Dispatchers.EDT) {
                toolWindow.update(selectedModule, component, listOfNotNull(component.elementClass))
            }
        }
    }

    private fun resolveElement(module: Module, psiFile: PsiFile, isClassNode: Boolean): PresentationLibraryElement? {
        val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module) ?: return null

        val elementClass: PsiClass = if (isClassNode) IdeaUtils.findPublicClass(psiFile) ?: return null
        else tapestryProject.findElementByTemplate(psiFile)?.elementClass ?: return null

        return try {
            PresentationLibraryElement.createProjectElementInstance(elementClass, tapestryProject)
        } catch (_: NotTapestryElementException) {
            // the selection is not a Tapestry element; previously this could escape for a FileNode, which
            // would now fail the coroutine rather than the Swing listener.
            null
        }
    }

    private fun modulesChanged() {
        subscribeToModules()

        val shouldShow = ModuleManager.getInstance(myProject).modules.any { TapestryUtils.isTapestryModule(it) }

        if (shouldShow && !shown) addMe()
        if (!shouldShow && shown) removeMe()
    }

    private fun addMe() {
        // The pane is also registered declaratively (<projectViewPane> in plugin.xml), so the platform may
        // already hold one under this id; `shown` only tracks our own add/remove. Adding a second time is a
        // logged error blamed on the plugin.
        val projectView = ProjectView.getInstance(myProject)
        if (projectView.getProjectViewPaneById(ID) == null) projectView.addProjectPane(this)
        shown = true
    }

    private fun removeMe() {
        ProjectView.getInstance(myProject).removeProjectPane(this)
        shown = false
    }

    companion object {
        private const val VIEW_TITLE = "Tapestry"
        private const val ID = "TapestryProjectView"

        /** Returns the project instance of this view pane. */
        fun getInstance(project: Project): TapestryProjectViewPane =
            ProjectView.getInstance(project).getProjectViewPaneById(ID) as TapestryProjectViewPane
    }
}
