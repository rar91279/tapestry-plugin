package com.intellij.tapestry.intellij.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.model.TapestryLibrary
import com.intellij.tapestry.core.model.externalizable.documentation.Home
import com.intellij.tapestry.core.model.externalizable.documentation.generationchain.AbstractDocumentationGenerator
import com.intellij.tapestry.core.model.externalizable.documentation.generationchain.CoreLibraryDocumentation
import com.intellij.tapestry.core.model.externalizable.documentation.generationchain.NavPageDocumentation
import com.intellij.tapestry.core.model.externalizable.documentation.generationchain.ServiceDocumentation
import com.intellij.tapestry.core.model.ioc.ModuleBuilder
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType
import com.intellij.tapestry.intellij.util.TapestryUtils
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.IOException
import java.util.ArrayDeque
import java.util.Deque
import java.util.TreeSet
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * The "Live Documentation" tool-window tab: a three-level browser rendered in an embedded Chromium
 * (JCEF) panel — Home (modules + core) → detail page (pages/components/mixins/services) → element doc.
 */
class DocumentationTab(private val project: Project) {

    private val htmlPanel = JCEFHtmlPanel(null as String?)

    // Same toolbar style as the Dependencies tab: left-packed icon ActionButtons driven by AnActions.
    private val presentations = PresentationFactory()
    private val navigateToElementAction = NavigateToElementAction()

    val mainPanel: JComponent = BorderLayoutPanel()

    /** The presentation element currently shown, for the Open-in-Editor action (`null` otherwise). */
    private var element: Any? = null
    /** GoTo Class target for pages without a presentation element (e.g. service pages); `null` otherwise. */
    private var classFqn: String? = null
    /** Re-renders whatever is currently shown (theme change, index-ready). */
    private var reload: Runnable? = null
    /** Previously shown views, for back navigation. */
    private val history: Deque<Runnable> = ArrayDeque()
    /** `<script>` bridging in-page links and the mouse back button to Java. */
    private var bridgeScript = ""
    /** Breadcrumb bar HTML for the current view, injected at the top of each page. */
    private var breadcrumbHtml = ""
    /** Notified with the shown presentation element (or `null`) so the Dependencies tab can follow. */
    private var elementListener: ((Any?) -> Unit)? = null

    init {
        // FlowLayout keeps the button at its natural size, packed left — matching the Dependencies toolbar.
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
        toolbar.add(toolbarButton(navigateToElementAction, "Navigate to Element"))

        Disposer.register(project, htmlPanel)
        // Let the browser shrink so the tool-window tabs stay visible in a short pane.
        htmlPanel.component.minimumSize = Dimension(0, 0)
        htmlPanel.component.preferredSize = Dimension(0, 0)
        (mainPanel as BorderLayoutPanel).addToTop(toolbar)
        mainPanel.addToCenter(htmlPanel.component)

        // Bridge in-page navigation links and the mouse back button back to Java.
        val navQuery = JBCefJSQuery.create(htmlPanel as JBCefBrowserBase)
        navQuery.addHandler { token ->
            ApplicationManager.getApplication().invokeLater { navigate(token) }
            null
        }
        val backQuery = JBCefJSQuery.create(htmlPanel as JBCefBrowserBase)
        backQuery.addHandler { _ ->
            ApplicationManager.getApplication().invokeLater { back() }
            null
        }
        bridgeScript = "<script>" +
                "function tapestryNav(t){" + navQuery.inject("t") + "}" +
                "function tapestryBack(){" + backQuery.inject("''") + "}" +
                "document.addEventListener('mouseup',function(e){if(e.button===3){e.preventDefault();tapestryBack();}});" +
                "</script>"

        // Re-render on IDE theme change so the docs track dark/light.
        ApplicationManager.getApplication().messageBus.connect(htmlPanel)
            .subscribe(LafManagerListener.TOPIC, LafManagerListener { reload?.run() })

        showHome()

        // Content needs the index; re-render once it's ready so it appears after startup indexing.
        DumbService.getInstance(project).runWhenSmart { reload?.run() }
    }

    private fun toolbarButton(action: AnAction, tooltip: String): ActionButton {
        val button = ActionButton(action, presentations.getPresentation(action), tooltip, Dimension(24, 24))
        button.toolTipText = tooltip
        return button
    }

    private fun setNavigateEnabled(enabled: Boolean) {
        presentations.getPresentation(navigateToElementAction).isEnabled = enabled
    }

    private inner class NavigateToElementAction :
        AnAction("Navigate to Element", "Navigate to the selected element class", AllIcons.Actions.PreviousOccurence) {
        override fun actionPerformed(e: AnActionEvent) = navigateToClass()
    }

    /** Sets a listener notified with the shown element (or `null`) whenever the view changes. */
    fun setElementListener(listener: (Any?) -> Unit) {
        elementListener = listener
    }

    private fun notifyElement(element: Any?) {
        elementListener?.invoke(element)
    }

    /** Navigate to the class of the shown presentation element, or the stored target class. */
    private fun navigateToClass() {
        val current = element
        if (current is PresentationLibraryElement) {
            val psiClass = (current.elementClass as IntellijJavaClassType).psiClass ?: return
            FileEditorManager.getInstance(project).openFile(psiClass.containingFile.virtualFile, true)
        } else if (classFqn != null) {
            openClass(classFqn!!)
        }
    }

    fun setElement(element: Any?) {
        this.element = element
    }

    /**
     * External entry point (project view / editor navigation): show a live element's documentation,
     * or the Home page when [element] is `null`.
     */
    fun showDocumentation(element: Any?) {
        if (element == null) {
            history.clear()
            showHome()
            return
        }

        val elementType = element as PresentationLibraryElement
        reload = Runnable { showDocumentation(element) }
        this.element = element
        setNavigateEnabled(true)

        setCrumbs(seg("Home", "home"), seg(elementType.name, ""))
        notifyElement(element)
        renderAsync { elementType.documentation }
    }

    // ---- Navigation ---------------------------------------------------------

    /** Handles a link click: remembers the current view, then dispatches the navigation token. */
    private fun navigate(token: String) {
        reload?.let { history.push(it) }
        dispatch(token)
    }

    private fun dispatch(token: String) {
        val parts = token.split("/")
        when (parts[0]) {
            "module" -> showModule(parts[1])
            "core" -> if (parts.size == 1) showCoreIndex() else showCoreElement(parts[1], parts[2])
            "el" -> showProjectElement(parts[1], parts[2], parts[3])
            "svc" -> showService(parts[1], parts[2])
            "library" -> showLibrary(parts[1], parts[2])
            "libsvc" -> showLibraryService(parts[1], parts[2], parts[3])
            "pom" -> openPom(parts[1], parts[2])
            "file" -> openFile(token.substring("file/".length))
            "class" -> openClass(parts[1])
            else -> showHome()
        }
    }

    /** Returns to the previously shown view. */
    private fun back() {
        if (!history.isEmpty()) history.pop().run()
    }

    private fun showHome() {
        element = null
        setNavigateEnabled(false)
        reload = Runnable { showHome() }
        setCrumbs(seg("Home", "home"))
        notifyElement(null)
        renderAsync { buildHomeHtml() }
    }

    private fun showModule(moduleName: String) {
        element = null
        setNavigateEnabled(false)
        reload = Runnable { showModule(moduleName) }
        setCrumbs(seg("Home", "home"), seg(moduleName, "module/$moduleName"))
        notifyElement(null)
        renderAsync { buildModuleHtml(moduleName) }
    }

    private fun showCoreIndex() {
        element = null
        setNavigateEnabled(false)
        reload = Runnable { showCoreIndex() }
        setCrumbs(seg("Home", "home"), seg("Core Library", "core"))
        notifyElement(null)
        renderAsync { CoreLibraryDocumentation.renderIndex() }
    }

    private fun showCoreElement(kind: String, name: String) {
        element = null
        setNavigateEnabled(false)
        reload = Runnable { showCoreElement(kind, name) }
        setCrumbs(seg("Home", "home"), seg("Core Library", "core"), seg(name, "core/$kind/$name"))
        notifyElement(null)
        renderAsync { CoreLibraryDocumentation.render(kind, name) }
    }

    private fun showProjectElement(moduleName: String, kind: String, name: String) {
        reload = Runnable { showProjectElement(moduleName, kind, name) }
        setCrumbs(seg("Home", "home"), seg(moduleName, "module/$moduleName"),
            seg(name, "el/$moduleName/$kind/$name"))

        ReadAction.nonBlocking<Array<Any?>> {
            val element = resolveElement(moduleName, kind, name)
            var html: String? = null
            if (element != null) {
                try {
                    html = element.documentation
                } catch (canceled: ProcessCanceledException) {
                    throw canceled
                } catch (ex: Exception) {
                    logger.warn("Failed to render element $moduleName/$kind/$name", ex)
                }
            }
            arrayOf(element, html)
        }
            .expireWith(htmlPanel)
            .finishOnUiThread(ModalityState.any()) { result ->
                element = result[0]
                setNavigateEnabled(element != null)
                notifyElement(element)
                render(result[1] as String?)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showService(moduleName: String, id: String) {
        element = null
        setNavigateEnabled(false)
        reload = Runnable { showService(moduleName, id) }
        setCrumbs(seg("Home", "home"), seg(moduleName, "module/$moduleName"), seg(id, "svc/$moduleName/$id"))
        notifyElement(null)
        renderServiceDoc { findService(moduleName, id) }
    }

    /**
     * Resolves a service off the EDT, then renders its doc and points the GoTo Class button at the
     * service implementation class (enabled only when the class is known).
     */
    private fun renderServiceDoc(resolver: () -> Home.ServiceDoc?) {
        notifyElement(null)
        ReadAction.nonBlocking<Array<Any?>> {
            try {
                val service = resolver()
                arrayOf(
                    if (service == null) null else ServiceDocumentation.render(service),
                    if (service == null) null else StringUtil.nullize(service.className))
            } catch (canceled: ProcessCanceledException) {
                throw canceled
            } catch (ex: Exception) {
                logger.warn("Failed to render service documentation", ex)
                arrayOf<Any?>(null, null)
            }
        }
            .expireWith(htmlPanel)
            .finishOnUiThread(ModalityState.any()) { result ->
                classFqn = result[1] as String?
                setNavigateEnabled(classFqn != null)
                render(result[0] as String?)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showLibrary(moduleName: String, moduleClass: String) {
        element = null
        setNavigateEnabled(false)
        reload = Runnable { showLibrary(moduleName, moduleClass) }
        setCrumbs(seg("Home", "home"),
            seg(StringUtil.getShortName(moduleClass), "library/$moduleName/$moduleClass"))
        notifyElement(null)
        renderAsync { buildLibraryHtml(moduleName, moduleClass) }
    }

    private fun showLibraryService(moduleName: String, moduleClass: String, id: String) {
        element = null
        setNavigateEnabled(false)
        reload = Runnable { showLibraryService(moduleName, moduleClass, id) }
        setCrumbs(seg("Home", "home"),
            seg(StringUtil.getShortName(moduleClass), "library/$moduleName/$moduleClass"),
            seg(id, "libsvc/$moduleName/$moduleClass/$id"))
        renderServiceDoc {
            val tapestryProject = tapestryProjectFor(moduleName) ?: return@renderServiceDoc null
            discoverLibraryServices(tapestryProject, moduleClass).firstOrNull { it.id == id }
        }
    }

    /** Opens a local file by path (template / message-catalog links). */
    private fun openFile(path: String) {
        val file = LocalFileSystem.getInstance().findFileByPath(path)
        if (file != null) OpenFileDescriptor(project, file).navigate(true)
    }

    /** Opens a class by fully-qualified name (service class links). */
    private fun openClass(fqn: String) {
        ReadAction.nonBlocking<com.intellij.psi.PsiClass?> {
            JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        }
            .expireWith(htmlPanel)
            .finishOnUiThread(ModalityState.any()) { psiClass ->
                if (psiClass != null && psiClass.isValid) psiClass.navigate(true)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    /**
     * Opens the project pom.xml declaring the given dependency, positioned at its
     * `<groupId>/<artifactId>` declaration.
     */
    private fun openPom(groupId: String, artifactId: String) {
        val dependency = Pattern.compile(
            "<groupId>\\s*" + Pattern.quote(groupId) + "\\s*</groupId>\\s*"
                    + "<artifactId>\\s*" + Pattern.quote(artifactId) + "\\s*</artifactId>")
        ReadAction.nonBlocking<Array<Any>?> {
            for (pom in FilenameIndex.getVirtualFilesByName("pom.xml", GlobalSearchScope.projectScope(project))) {
                val fileText: String = try {
                    VfsUtilCore.loadText(pom)
                } catch (e: IOException) {
                    continue
                }
                val m = dependency.matcher(fileText)
                if (m.find()) {
                    // Navigate by line (separator-independent) rather than char offset.
                    return@nonBlocking arrayOf<Any>(pom, StringUtil.offsetToLineNumber(fileText, m.start()))
                }
            }
            null
        }
            .expireWith(htmlPanel)
            .finishOnUiThread(ModalityState.any()) { target ->
                if (target != null) {
                    OpenFileDescriptor(project, target[0] as VirtualFile, target[1] as Int, 0).navigate(true)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    // ---- Page builders (run inside a background read action) ----------------

    private fun buildHomeHtml(): String {
        val modules = ArrayList<NavPageDocumentation.Entry>()
        for (module in ModuleManager.getInstance(project).modules) {
            // Only Tapestry-recognized modules are listed; non-Tapestry project dirs are skipped.
            if (!TapestryUtils.isTapestryModule(module)) continue
            modules.add(NavPageDocumentation.Entry(module.name, "module/" + module.name, "", "Tapestry"))
        }

        val sections = ArrayList<NavPageDocumentation.Section>()
        sections.add(NavPageDocumentation.Section("Tapestry Modules", modules))

        // Tapestry modules contributed by classpath library jars (Tapestry-Module-Classes manifest).
        val libs = LinkedHashMap<String, Array<String>>() // moduleClass -> [ownerModuleName, mavenInfo]
        for (module in ModuleManager.getInstance(project).modules) {
            for (lib in TapestryUtils.getClasspathLibraryModules(module)) {
                libs.putIfAbsent(lib.moduleClass(), arrayOf(module.name, lib.mavenInfo()))
            }
        }
        if (libs.isNotEmpty()) {
            val libraryEntries = ArrayList<NavPageDocumentation.Entry>()
            for ((moduleClass, value) in libs) {
                val owner = value[0]
                val mavenInfo = value[1]
                val gav = mavenInfo.split(":")
                val pomToken = if (gav.size >= 2) "pom/" + gav[0] + "/" + gav[1] else ""
                libraryEntries.add(NavPageDocumentation.Entry(
                    libraryDisplayName(moduleClass), "library/$owner/$moduleClass", mavenInfo, "", pomToken))
            }
            sections.add(NavPageDocumentation.Section("Tapestry Libraries", libraryEntries))
        }

        sections.add(NavPageDocumentation.Section("Tapestry Core", listOf(
            NavPageDocumentation.Entry("Core Library", "core",
                "Built-in Tapestry pages, components and mixins."))))
        return NavPageDocumentation.render("Tapestry Documentation", sections)
    }

    private fun buildModuleHtml(moduleName: String): String? {
        val module = findModule(moduleName) ?: return null
        val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module) ?: return null

        val library = tapestryProject.applicationLibrary

        val sections = ArrayList<NavPageDocumentation.Section>()
        sections.add(elementSection("Pages", moduleName, "pages",
            if (library == null) emptyList() else library.pages.keys))
        sections.add(elementSection("Components", moduleName, "components",
            if (library == null) emptyList() else library.components.keys))
        sections.add(elementSection("Mixins", moduleName, "mixins",
            if (library == null) emptyList() else library.mixins.keys))

        val services = ArrayList<NavPageDocumentation.Entry>()
        for (service in discoverServices(module)) {
            services.add(NavPageDocumentation.Entry(service.id, "svc/$moduleName/" + service.id,
                NavPageDocumentation.summary(service.description)))
        }
        sections.add(NavPageDocumentation.Section("Services", services))

        return NavPageDocumentation.render(moduleName, sections)
    }

    private fun buildLibraryHtml(moduleName: String, moduleClass: String): String? {
        val tapestryProject = tapestryProjectFor(moduleName) ?: return null

        val sections = ArrayList<NavPageDocumentation.Section>()

        val services = ArrayList<NavPageDocumentation.Entry>()
        for (service in discoverLibraryServices(tapestryProject, moduleClass)) {
            services.add(NavPageDocumentation.Entry(service.id,
                "libsvc/$moduleName/$moduleClass/" + service.id,
                NavPageDocumentation.summary(service.description)))
        }
        sections.add(NavPageDocumentation.Section("Services", services))

        // Components/pages/mixins the library contributes, if any (listed, not yet drillable).
        val base = TapestryUtils.rootPackageForModuleClass(moduleClass)
        if (base != null) {
            val library = TapestryLibrary(TapestryProject.APPLICATION_LIBRARY_ID, base, tapestryProject)
            sections.add(nameSection("Pages", library.pages.keys))
            sections.add(nameSection("Components", library.components.keys))
            sections.add(nameSection("Mixins", library.mixins.keys))
        }

        val maven = mavenInfoFor(moduleName, moduleClass)
        val gav = maven.split(":")
        val pomToken = if (gav.size >= 2) "pom/" + gav[0] + "/" + gav[1] else ""
        return NavPageDocumentation.render(libraryDisplayName(moduleClass), maven, pomToken, sections)
    }

    private fun mavenInfoFor(moduleName: String, moduleClass: String): String {
        val module = findModule(moduleName)
        if (module != null) {
            for (lib in TapestryUtils.getClasspathLibraryModules(module)) {
                if (lib.moduleClass() == moduleClass) return lib.mavenInfo()
            }
        }
        return ""
    }

    /** A section of plain (non-clickable) element names. */
    private fun nameSection(title: String, names: Iterable<String>): NavPageDocumentation.Section {
        val entries = ArrayList<NavPageDocumentation.Entry>()
        for (name in TreeSet(names.toList())) entries.add(NavPageDocumentation.Entry(name, "", ""))
        return NavPageDocumentation.Section(title, entries)
    }

    private fun elementSection(title: String, moduleName: String, kind: String,
                               names: Iterable<String>): NavPageDocumentation.Section {
        val entries = ArrayList<NavPageDocumentation.Entry>()
        for (name in TreeSet(names.toList())) {
            entries.add(NavPageDocumentation.Entry(name, "el/$moduleName/$kind/$name", ""))
        }
        return NavPageDocumentation.Section(title, entries)
    }

    // ---- Resolution helpers -------------------------------------------------

    private fun resolveElement(moduleName: String, kind: String, name: String): PresentationLibraryElement? {
        val module = findModule(moduleName)
        val tapestryProject = if (module == null) null else TapestryModuleSupportLoader.getTapestryProject(module)
        if (tapestryProject == null) return null

        return when (kind) {
            "pages" -> tapestryProject.findPage(name)
            "components" -> tapestryProject.findComponent(name)
            "mixins" -> tapestryProject.findMixin(name)
            else -> null
        }
    }

    private fun findService(moduleName: String, id: String): Home.ServiceDoc? {
        val module = findModule(moduleName) ?: return null
        return discoverServices(module).firstOrNull { it.id == id }
    }

    private fun findModule(moduleName: String): Module? =
        ModuleManager.getInstance(project).modules.firstOrNull { it.name == moduleName }

    private fun tapestryProjectFor(moduleName: String): TapestryProject? {
        val module = findModule(moduleName) ?: return null
        return TapestryModuleSupportLoader.getTapestryProject(module)
    }

    /**
     * Discovers the services declared by a module's `<appPackage>.services.*Module` builders.
     * Runs inside the caller's (background) read action.
     */
    private fun discoverServices(module: Module): List<Home.ServiceDoc> {
        if (DumbService.isDumb(project)) return emptyList()

        val services = ArrayList<Home.ServiceDoc>()
        try {
            val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module)
            val root = tapestryProject?.applicationRootPackage ?: return services

            for (builder in tapestryProject.javaTypeFinder.findTypesInPackageRecursively("$root.services", true)) {
                if (!builder.fullyQualifiedName.endsWith("Module")) continue
                addServiceDocs(builder, tapestryProject, services)
            }
        } catch (canceled: ProcessCanceledException) {
            throw canceled
        } catch (ex: Exception) {
            logger.warn("Failed to discover services for module " + module.name, ex)
        }
        services.sortWith { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.id, b.id) }
        return services
    }

    /**
     * Discovers the services declared by a single library module class (from a classpath jar).
     * Runs inside the caller's (background) read action.
     */
    private fun discoverLibraryServices(tapestryProject: TapestryProject, moduleClass: String): List<Home.ServiceDoc> {
        if (DumbService.isDumb(project)) return emptyList()

        val services = ArrayList<Home.ServiceDoc>()
        try {
            val builder = tapestryProject.javaTypeFinder.findType(moduleClass, true)
            if (builder != null) addServiceDocs(builder, tapestryProject, services)
        } catch (canceled: ProcessCanceledException) {
            throw canceled
        } catch (ex: Exception) {
            logger.warn("Failed to discover services for library module $moduleClass", ex)
        }
        services.sortWith { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.id, b.id) }
        return services
    }

    // ---- Rendering ----------------------------------------------------------

    /** Runs an HTML supplier off the EDT (PSI/index access), then renders the result on the EDT. */
    private fun renderAsync(supplier: () -> String?) {
        ReadAction.nonBlocking<String?> {
            try {
                supplier()
            } catch (canceled: ProcessCanceledException) {
                throw canceled
            } catch (ex: Exception) {
                logger.warn("Failed to render documentation", ex)
                null
            }
        }
            .expireWith(htmlPanel)
            .finishOnUiThread(ModalityState.any()) { html -> render(html) }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    /**
     * Renders HTML into the panel, stamping the current IDE theme (JCEF doesn't reliably map
     * prefers-color-scheme to the IDE's dark/light mode) and injecting the navigation bridge.
     */
    private fun render(html: String?) {
        if (html == null) {
            clear()
            return
        }
        val themeClass = if (JBColor.isBright()) "light" else "dark"
        val out = html
            .replace("<body", "<body class=\"$themeClass\"")
            .replace("<div class=\"page\">", "<div class=\"page\">$breadcrumbHtml")
            .replace("</body>", "$bridgeScript</body>")
        htmlPanel.setHtml(out)
    }

    private fun clear() {
        htmlPanel.setHtml("")
    }

    // ---- Breadcrumbs --------------------------------------------------------

    /** Builds the breadcrumb bar; the last segment is the current (non-clickable) page. */
    private fun setCrumbs(vararg segments: Array<String>) {
        val sb = StringBuilder("<nav class=\"crumbs\">")
        sb.append("<img class=\"crumb-logo\" src=\"")
            .append(AbstractDocumentationGenerator.logo())
            .append("\" alt=\"Tapestry\">")
        for (i in segments.indices) {
            if (i > 0) sb.append("<span class=\"sep\">/</span>")

            val label = escape(segments[i][0])
            val token = segments[i][1]
            val last = i == segments.size - 1

            if (last || token.isEmpty()) {
                sb.append("<span class=\"current\">").append(label).append("</span>")
            } else {
                sb.append("<a href=\"#\" onclick=\"tapestryNav('").append(token).append("');return false;\">")
                    .append(label).append("</a>")
            }
        }
        breadcrumbHtml = sb.append("</nav>").toString()
    }

    private fun addServiceDocs(builder: IJavaClassType, tapestryProject: TapestryProject,
                               out: MutableList<Home.ServiceDoc>) {
        for (service in ModuleBuilder(builder, tapestryProject).services) {
            val serviceClass = service.serviceClass
            out.add(Home.ServiceDoc(
                service.id,
                serviceClass?.fullyQualifiedName ?: "",
                service.scope,
                service.isEagerLoad,
                serviceClass?.documentation ?: ""))
        }
    }

    companion object {
        private val logger = Logger.getInstance(DocumentationTab::class.java)

        private fun seg(label: String, token: String): Array<String> = arrayOf(label, token)

        /** Library module class → display name: strip the `Module` suffix and split camel-case
         * (e.g. `TapestryConfigModule` → `Tapestry Config`). */
        private fun libraryDisplayName(moduleClass: String): String {
            val name = StringUtil.trimEnd(StringUtil.getShortName(moduleClass), "Module")
            val spaced = name.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ").trim()
            return spaced.ifEmpty { StringUtil.getShortName(moduleClass) }
        }

        private fun escape(text: String): String =
            text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
