package com.intellij.tapestry.intellij.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.tapestry.core.TapestryProject;
import com.intellij.tapestry.core.java.IJavaClassType;
import com.intellij.tapestry.core.model.TapestryLibrary;
import com.intellij.tapestry.core.model.externalizable.documentation.Home;
import com.intellij.tapestry.core.model.externalizable.documentation.generationchain.CoreLibraryDocumentation;
import com.intellij.tapestry.core.model.externalizable.documentation.generationchain.NavPageDocumentation;
import com.intellij.tapestry.core.model.externalizable.documentation.generationchain.ServiceDocumentation;
import com.intellij.tapestry.core.model.ioc.ModuleBuilder;
import com.intellij.tapestry.core.model.ioc.Service;
import com.intellij.tapestry.core.model.presentation.PresentationLibraryElement;
import com.intellij.tapestry.intellij.TapestryModuleSupportLoader;
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType;
import com.intellij.tapestry.intellij.util.TapestryUtils;
import com.intellij.ui.JBColor;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.concurrency.AppExecutorUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "Live Documentation" tool-window tab: a three-level browser rendered in an embedded Chromium
 * (JCEF) panel — Home (modules + core) → detail page (pages/components/mixins/services) → element doc.
 */
public class DocumentationTab {

    private JButton _homeButton;
    private JButton _reloadButton;
    private JButton _backButton;
    private JButton _forwardButton;
    private JButton _goButton;
    private JTextField _text;
    private JButton _docButton;
    private JPanel _mainPanel;

    private static final Logger _logger = Logger.getInstance(DocumentationTab.class);

    private final JCEFHtmlPanel _htmlPanel = new JCEFHtmlPanel(null);
    private JButton _classButton;
    private final Project _project;
    /** The presentation element currently shown, for the GoTo Class action ({@code null} otherwise). */
    private Object _element;
    /** GoTo Class target for pages without a presentation element (e.g. service pages); {@code null} otherwise. */
    private String _classFqn;
    /** Re-renders whatever is currently shown (theme change, index-ready). */
    private Runnable _reload;
    /** Previously shown views, for back navigation. */
    private final Deque<Runnable> _history = new ArrayDeque<>();
    /** {@code <script>} bridging in-page links and the mouse back button to Java. */
    private String _bridgeScript = "";
    /** Breadcrumb bar HTML for the current view, injected at the top of each page. */
    private String _breadcrumbHtml = "";
    /** Notified with the shown presentation element (or {@code null}) so the Dependencies tab can follow. */
    private java.util.function.Consumer<Object> _elementListener;

    public DocumentationTab(Project project) {
        _project = project;

        // Navigation is via in-page breadcrumbs and the mouse back button; the toolbar keeps only GoTo Class.
        _homeButton.setVisible(false);
        _backButton.setVisible(false);
        _forwardButton.setVisible(false);
        _reloadButton.setVisible(false);
        _goButton.setVisible(false);
        _docButton.setVisible(false);
        _text.setVisible(false);

        // Match the Dependencies tab's "Navigate to Element" button (up-arrow icon).
        _classButton.setText("");
        _classButton.setIcon(AllIcons.Actions.PreviousOccurence);
        _classButton.setToolTipText("Navigate to Element Class");

        Disposer.register(project, _htmlPanel);
        // Let the browser shrink so the tool-window tabs stay visible in a short pane.
        _htmlPanel.getComponent().setMinimumSize(new Dimension(0, 0));
        _htmlPanel.getComponent().setPreferredSize(new Dimension(0, 0));
        _mainPanel.add(_htmlPanel.getComponent(), new GridConstraints(1, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW,
                null, null, null));

        // Bridge in-page navigation links and the mouse back button back to Java.
        JBCefJSQuery navQuery = JBCefJSQuery.create((JBCefBrowserBase) _htmlPanel);
        navQuery.addHandler(token -> {
            ApplicationManager.getApplication().invokeLater(() -> navigate(token));
            return null;
        });
        JBCefJSQuery backQuery = JBCefJSQuery.create((JBCefBrowserBase) _htmlPanel);
        backQuery.addHandler(request -> {
            ApplicationManager.getApplication().invokeLater(this::back);
            return null;
        });
        _bridgeScript = "<script>"
                + "function tapestryNav(t){" + navQuery.inject("t") + "}"
                + "function tapestryBack(){" + backQuery.inject("''") + "}"
                + "document.addEventListener('mouseup',function(e){if(e.button===3){e.preventDefault();tapestryBack();}});"
                + "</script>";

        // Re-render on IDE theme change so the docs track dark/light.
        ApplicationManager.getApplication().getMessageBus().connect(_htmlPanel)
                .subscribe(LafManagerListener.TOPIC,
                        (LafManagerListener) _ -> { if (_reload != null) _reload.run(); });

        showHome();

        // Content needs the index; re-render once it's ready so it appears after startup indexing.
        DumbService.getInstance(project).runWhenSmart(() -> {
            if (_reload != null)
                _reload.run();
        });

        _homeButton.addActionListener(e -> {
            _history.clear();
            showHome();
        });

        _backButton.addActionListener(e -> back());

        _classButton.addActionListener(e -> navigateToClass());
    }

    public JComponent getMainPanel() {
        return _mainPanel;
    }

    /** Sets a listener notified with the shown element (or {@code null}) whenever the view changes. */
    public void setElementListener(java.util.function.Consumer<Object> listener) {
        _elementListener = listener;
    }

    private void notifyElement(Object element) {
        if (_elementListener != null)
            _elementListener.accept(element);
    }

    /** Navigate to the class of the shown presentation element, or the stored target class. */
    protected void navigateToClass() {
        if (_element != null) {
            PresentationLibraryElement elementType = (PresentationLibraryElement) _element;
            FileEditorManager.getInstance(_project).openFile(
                    ((IntellijJavaClassType) elementType.getElementClass()).getPsiClass().getContainingFile().getVirtualFile(),
                    true);
        }
        else if (_classFqn != null) {
            openClass(_classFqn);
        }
    }

    protected void setElement(Object element) {
        _element = element;
    }

    /**
     * External entry point (project view / editor navigation): show a live element's documentation,
     * or the Home page when {@code element} is {@code null}.
     */
    protected void showDocumentation(Object element) {
        if (element == null) {
            _history.clear();
            showHome();
            return;
        }

        PresentationLibraryElement elementType = (PresentationLibraryElement) element;
        _reload = () -> showDocumentation(element);
        _element = element;
        _classButton.setEnabled(true);

        String library = elementType.getLibrary().getId();
        if (library.equals(TapestryProject.APPLICATION_LIBRARY_ID))
            _text.setText("ldp://App : " + elementType.getElementClass().getFullyQualifiedName());
        else
            _text.setText("ldp://Lib : " + library + " : " + elementType.getElementClass().getFullyQualifiedName());

        setCrumbs(seg("Home", "home"), seg(elementType.getName(), ""));
        notifyElement(element);
        renderAsync(elementType::getDocumentation);
    }

    // ---- Navigation ---------------------------------------------------------

    /** Handles a link click: remembers the current view, then dispatches the navigation token. */
    private void navigate(String token) {
        if (_reload != null)
            _history.push(_reload);
        dispatch(token);
    }

    private void dispatch(String token) {
        String[] parts = token.split("/", -1);
        switch (parts[0]) {
            case "module" -> showModule(parts[1]);
            case "core" -> {
                if (parts.length == 1) showCoreIndex();
                else showCoreElement(parts[1], parts[2]);
            }
            case "el" -> showProjectElement(parts[1], parts[2], parts[3]);
            case "svc" -> showService(parts[1], parts[2]);
            case "library" -> showLibrary(parts[1], parts[2]);
            case "libsvc" -> showLibraryService(parts[1], parts[2], parts[3]);
            case "pom" -> openPom(parts[1], parts[2]);
            case "file" -> openFile(token.substring("file/".length()));
            case "class" -> openClass(parts[1]);
            default -> showHome();
        }
    }

    /** Returns to the previously shown view. */
    private void back() {
        if (!_history.isEmpty())
            _history.pop().run();
    }

    private void showHome() {
        _element = null;
        _classButton.setEnabled(false);
        _text.setText("ldp://Home");
        _reload = this::showHome;
        setCrumbs(seg("Home", "home"));
        notifyElement(null);
        renderAsync(this::buildHomeHtml);
    }

    private void showModule(String moduleName) {
        _element = null;
        _classButton.setEnabled(false);
        _text.setText("ldp://" + moduleName);
        _reload = () -> showModule(moduleName);
        setCrumbs(seg("Home", "home"), seg(moduleName, "module/" + moduleName));
        notifyElement(null);
        renderAsync(() -> buildModuleHtml(moduleName));
    }

    private void showCoreIndex() {
        _element = null;
        _classButton.setEnabled(false);
        _text.setText("ldp://Core");
        _reload = this::showCoreIndex;
        setCrumbs(seg("Home", "home"), seg("Core Library", "core"));
        notifyElement(null);
        renderAsync(CoreLibraryDocumentation::renderIndex);
    }

    private void showCoreElement(String kind, String name) {
        _element = null;
        _classButton.setEnabled(false);
        _text.setText("ldp://Core : " + name);
        _reload = () -> showCoreElement(kind, name);
        setCrumbs(seg("Home", "home"), seg("Core Library", "core"), seg(name, "core/" + kind + "/" + name));
        notifyElement(null);
        renderAsync(() -> CoreLibraryDocumentation.render(kind, name));
    }

    private void showProjectElement(String moduleName, String kind, String name) {
        _text.setText("ldp://" + moduleName + " : " + name);
        _reload = () -> showProjectElement(moduleName, kind, name);
        setCrumbs(seg("Home", "home"), seg(moduleName, "module/" + moduleName),
                seg(name, "el/" + moduleName + "/" + kind + "/" + name));

        ReadAction.nonBlocking(() -> {
                    PresentationLibraryElement element = resolveElement(moduleName, kind, name);
                    String html = null;
                    if (element != null) {
                        try {
                            html = element.getDocumentation();
                        } catch (ProcessCanceledException canceled) {
                            throw canceled;
                        } catch (Exception ex) {
                            _logger.warn("Failed to render element " + moduleName + "/" + kind + "/" + name, ex);
                        }
                    }
                    return new Object[]{element, html};
                })
                .expireWith(_htmlPanel)
                .finishOnUiThread(ModalityState.any(), result -> {
                    _element = result[0];
                    _classButton.setEnabled(_element != null);
                    notifyElement(_element);
                    render((String) result[1]);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void showService(String moduleName, String id) {
        _element = null;
        _classButton.setEnabled(false);
        _text.setText("ldp://" + moduleName + " : " + id);
        _reload = () -> showService(moduleName, id);
        setCrumbs(seg("Home", "home"), seg(moduleName, "module/" + moduleName),
                seg(id, "svc/" + moduleName + "/" + id));
        notifyElement(null);
        renderServiceDoc(() -> findService(moduleName, id));
    }

    /**
     * Resolves a service off the EDT, then renders its doc and points the GoTo Class button at the
     * service implementation class (enabled only when the class is known).
     */
    private void renderServiceDoc(Callable<Home.ServiceDoc> resolver) {
        notifyElement(null);
        ReadAction.nonBlocking(() -> {
                    try {
                        Home.ServiceDoc service = resolver.call();
                        return new Object[]{
                                service == null ? null : ServiceDocumentation.render(service),
                                service == null ? null : StringUtil.nullize(service.getClassName())};
                    } catch (ProcessCanceledException canceled) {
                        throw canceled;
                    } catch (Exception ex) {
                        _logger.warn("Failed to render service documentation", ex);
                        return new Object[]{null, null};
                    }
                })
                .expireWith(_htmlPanel)
                .finishOnUiThread(ModalityState.any(), result -> {
                    _classFqn = (String) result[1];
                    _classButton.setEnabled(_classFqn != null);
                    render((String) result[0]);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void showLibrary(String moduleName, String moduleClass) {
        _element = null;
        _classButton.setEnabled(false);
        _text.setText("ldp://Lib : " + moduleClass);
        _reload = () -> showLibrary(moduleName, moduleClass);
        setCrumbs(seg("Home", "home"),
                seg(StringUtil.getShortName(moduleClass), "library/" + moduleName + "/" + moduleClass));
        notifyElement(null);
        renderAsync(() -> buildLibraryHtml(moduleName, moduleClass));
    }

    private void showLibraryService(String moduleName, String moduleClass, String id) {
        _element = null;
        _classButton.setEnabled(false);
        _text.setText("ldp://Lib : " + moduleClass + " : " + id);
        _reload = () -> showLibraryService(moduleName, moduleClass, id);
        setCrumbs(seg("Home", "home"),
                seg(StringUtil.getShortName(moduleClass), "library/" + moduleName + "/" + moduleClass),
                seg(id, "libsvc/" + moduleName + "/" + moduleClass + "/" + id));
        renderServiceDoc(() -> {
            TapestryProject tapestryProject = tapestryProjectFor(moduleName);
            if (tapestryProject == null)
                return null;
            for (Home.ServiceDoc service : discoverLibraryServices(tapestryProject, moduleClass))
                if (service.getId().equals(id))
                    return service;
            return null;
        });
    }

    /** Opens a local file by path (template / message-catalog links). */
    private void openFile(String path) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file != null)
            new OpenFileDescriptor(_project, file).navigate(true);
    }

    /** Opens a class by fully-qualified name (service class links). */
    private void openClass(String fqn) {
        ReadAction.nonBlocking(() ->
                        JavaPsiFacade.getInstance(_project).findClass(fqn, GlobalSearchScope.allScope(_project)))
                .expireWith(_htmlPanel)
                .finishOnUiThread(ModalityState.any(), psiClass -> {
                    if (psiClass != null && psiClass.isValid())
                        psiClass.navigate(true);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    /**
     * Opens the project pom.xml declaring the given dependency, positioned at its
     * {@code <groupId>/<artifactId>} declaration. Matches the groupId+artifactId pair so it lands on
     * the real dependency rather than a same-named module, plugin, or the pom's own artifactId.
     */
    private void openPom(String groupId, String artifactId) {
        Pattern dependency = Pattern.compile(
                "<groupId>\\s*" + Pattern.quote(groupId) + "\\s*</groupId>\\s*"
                        + "<artifactId>\\s*" + Pattern.quote(artifactId) + "\\s*</artifactId>");
        ReadAction.nonBlocking(() -> {
                    for (VirtualFile pom : FilenameIndex.getVirtualFilesByName("pom.xml", GlobalSearchScope.projectScope(_project))) {
                        String text;
                        try {
                            text = VfsUtilCore.loadText(pom);
                        } catch (IOException e) {
                            continue;
                        }
                        Matcher m = dependency.matcher(text);
                        if (m.find())
                            // Navigate by line (separator-independent) rather than char offset.
                            return new Object[]{pom, StringUtil.offsetToLineNumber(text, m.start())};
                    }
                    return null;
                })
                .expireWith(_htmlPanel)
                .finishOnUiThread(ModalityState.any(), target -> {
                    if (target != null)
                        new OpenFileDescriptor(_project, (VirtualFile) target[0], (Integer) target[1], 0).navigate(true);
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    // ---- Page builders (run inside a background read action) ----------------

    private String buildHomeHtml() {
        List<NavPageDocumentation.Entry> modules = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(_project).getModules()) {
            // Only Tapestry-recognized modules are listed; non-Tapestry project dirs are skipped.
            if (!TapestryUtils.isTapestryModule(module)) {
                continue;
            }
            modules.add(new NavPageDocumentation.Entry(
                    module.getName(),
                    "module/" + module.getName(),
                    "",
                    "Tapestry"));
        }

        List<NavPageDocumentation.Section> sections = new ArrayList<>();
        sections.add(new NavPageDocumentation.Section("Tapestry Modules", modules));

        // Tapestry modules contributed by classpath library jars (Tapestry-Module-Classes manifest).
        Map<String, String[]> libs = new LinkedHashMap<>(); // moduleClass -> [ownerModuleName, mavenInfo]
        for (Module module : ModuleManager.getInstance(_project).getModules()) {
            for (TapestryUtils.LibraryModule lib : TapestryUtils.getClasspathLibraryModules(module)) {
                libs.putIfAbsent(lib.moduleClass(), new String[]{module.getName(), lib.mavenInfo()});
            }
        }
        if (!libs.isEmpty()) {
            List<NavPageDocumentation.Entry> libraryEntries = new ArrayList<>();
            for (Map.Entry<String, String[]> e : libs.entrySet()) {
                String moduleClass = e.getKey();
                String owner = e.getValue()[0];
                String mavenInfo = e.getValue()[1];
                String[] gav = mavenInfo.split(":");
                String pomToken = gav.length >= 2 ? "pom/" + gav[0] + "/" + gav[1] : "";
                libraryEntries.add(new NavPageDocumentation.Entry(
                        libraryDisplayName(moduleClass),
                        "library/" + owner + "/" + moduleClass,
                        mavenInfo, "", pomToken));
            }
            sections.add(new NavPageDocumentation.Section("Tapestry Libraries", libraryEntries));
        }

        sections.add(new NavPageDocumentation.Section("Tapestry Core", List.of(
                new NavPageDocumentation.Entry("Core Library", "core",
                        "Built-in Tapestry pages, components and mixins."))));
        return NavPageDocumentation.render("Tapestry Documentation", sections);
    }

    private String buildModuleHtml(String moduleName) {
        Module module = findModule(moduleName);
        TapestryProject tapestryProject = module == null ? null : TapestryModuleSupportLoader.getTapestryProject(module);
        if (tapestryProject == null)
            return null;

        TapestryLibrary library = tapestryProject.getApplicationLibrary();

        List<NavPageDocumentation.Section> sections = new ArrayList<>();
        sections.add(elementSection("Pages", moduleName, "pages",
                library == null ? List.of() : library.getPages().keySet()));
        sections.add(elementSection("Components", moduleName, "components",
                library == null ? List.of() : library.getComponents().keySet()));
        sections.add(elementSection("Mixins", moduleName, "mixins",
                library == null ? List.of() : library.getMixins().keySet()));

        List<NavPageDocumentation.Entry> services = new ArrayList<>();
        for (Home.ServiceDoc service : discoverServices(module)) {
            services.add(new NavPageDocumentation.Entry(service.getId(),
                    "svc/" + moduleName + "/" + service.getId(),
                    NavPageDocumentation.summary(service.getDescription())));
        }
        sections.add(new NavPageDocumentation.Section("Services", services));

        return NavPageDocumentation.render(moduleName, sections);
    }

    private String buildLibraryHtml(String moduleName, String moduleClass) {
        TapestryProject tapestryProject = tapestryProjectFor(moduleName);
        if (tapestryProject == null)
            return null;

        List<NavPageDocumentation.Section> sections = new ArrayList<>();

        List<NavPageDocumentation.Entry> services = new ArrayList<>();
        for (Home.ServiceDoc service : discoverLibraryServices(tapestryProject, moduleClass)) {
            services.add(new NavPageDocumentation.Entry(service.getId(),
                    "libsvc/" + moduleName + "/" + moduleClass + "/" + service.getId(),
                    NavPageDocumentation.summary(service.getDescription())));
        }
        sections.add(new NavPageDocumentation.Section("Services", services));

        // Components/pages/mixins the library contributes, if any (listed, not yet drillable).
        String base = TapestryUtils.rootPackageForModuleClass(moduleClass);
        if (base != null) {
            TapestryLibrary library = new TapestryLibrary(TapestryProject.APPLICATION_LIBRARY_ID, base, tapestryProject);
            sections.add(nameSection("Pages", library.getPages().keySet()));
            sections.add(nameSection("Components", library.getComponents().keySet()));
            sections.add(nameSection("Mixins", library.getMixins().keySet()));
        }

        String maven = mavenInfoFor(moduleName, moduleClass);
        String[] gav = maven.split(":");
        String pomToken = gav.length >= 2 ? "pom/" + gav[0] + "/" + gav[1] : "";
        return NavPageDocumentation.render(libraryDisplayName(moduleClass), maven, pomToken, sections);
    }

    /** Library module class &rarr; display name: strip the {@code Module} suffix and split camel-case
     * (e.g. {@code TapestryConfigModule} &rarr; {@code Tapestry Config}). */
    private static String libraryDisplayName(String moduleClass) {
        String name = StringUtil.trimEnd(StringUtil.getShortName(moduleClass), "Module");
        String spaced = name.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ").trim();
        return spaced.isEmpty() ? StringUtil.getShortName(moduleClass) : spaced;
    }

    private String mavenInfoFor(String moduleName, String moduleClass) {
        Module module = findModule(moduleName);
        if (module != null)
            for (TapestryUtils.LibraryModule lib : TapestryUtils.getClasspathLibraryModules(module))
                if (lib.moduleClass().equals(moduleClass))
                    return lib.mavenInfo();
        return "";
    }

    /** A section of plain (non-clickable) element names. */
    private NavPageDocumentation.Section nameSection(String title, Iterable<String> names) {
        List<NavPageDocumentation.Entry> entries = new ArrayList<>();
        for (String name : new TreeSet<>(toList(names)))
            entries.add(new NavPageDocumentation.Entry(name, "", ""));
        return new NavPageDocumentation.Section(title, entries);
    }

    private NavPageDocumentation.Section elementSection(String title, String moduleName, String kind,
                                                        Iterable<String> names) {
        List<NavPageDocumentation.Entry> entries = new ArrayList<>();
        for (String name : new TreeSet<>(toList(names)))
            entries.add(new NavPageDocumentation.Entry(name, "el/" + moduleName + "/" + kind + "/" + name, ""));
        return new NavPageDocumentation.Section(title, entries);
    }

    // ---- Resolution helpers -------------------------------------------------

    private PresentationLibraryElement resolveElement(String moduleName, String kind, String name) {
        Module module = findModule(moduleName);
        TapestryProject tapestryProject = module == null ? null : TapestryModuleSupportLoader.getTapestryProject(module);
        if (tapestryProject == null)
            return null;

        return switch (kind) {
            case "pages" -> tapestryProject.findPage(name);
            case "components" -> tapestryProject.findComponent(name);
            case "mixins" -> tapestryProject.findMixin(name);
            default -> null;
        };
    }

    private Home.ServiceDoc findService(String moduleName, String id) {
        Module module = findModule(moduleName);
        if (module == null)
            return null;
        for (Home.ServiceDoc service : discoverServices(module))
            if (service.getId().equals(id))
                return service;
        return null;
    }

    private Module findModule(String moduleName) {
        for (Module module : ModuleManager.getInstance(_project).getModules())
            if (module.getName().equals(moduleName))
                return module;
        return null;
    }

    private TapestryProject tapestryProjectFor(String moduleName) {
        Module module = findModule(moduleName);
        return module == null ? null : TapestryModuleSupportLoader.getTapestryProject(module);
    }

    /**
     * Discovers the services declared by a module's {@code <appPackage>.services.*Module} builders.
     * Runs inside the caller's (background) read action.
     */
    private List<Home.ServiceDoc> discoverServices(Module module) {
        if (DumbService.isDumb(_project))
            return List.of();

        List<Home.ServiceDoc> services = new ArrayList<>();
        try {
            TapestryProject tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module);
            String root = tapestryProject == null ? null : tapestryProject.getApplicationRootPackage();
            if (root == null)
                return services;

            for (IJavaClassType builder : tapestryProject.getJavaTypeFinder()
                    .findTypesInPackageRecursively(root + ".services", true)) {
                if (!builder.getFullyQualifiedName().endsWith("Module"))
                    continue;
                addServiceDocs(builder, tapestryProject, services);
            }
        } catch (ProcessCanceledException canceled) {
            throw canceled;
        } catch (Exception ex) {
            _logger.warn("Failed to discover services for module " + module.getName(), ex);
        }
        services.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getId(), b.getId()));
        return services;
    }

    /**
     * Discovers the services declared by a single library module class (from a classpath jar).
     * Runs inside the caller's (background) read action.
     */
    private List<Home.ServiceDoc> discoverLibraryServices(TapestryProject tapestryProject, String moduleClass) {
        if (DumbService.isDumb(_project))
            return List.of();

        List<Home.ServiceDoc> services = new ArrayList<>();
        try {
            IJavaClassType builder = tapestryProject.getJavaTypeFinder().findType(moduleClass, true);
            if (builder != null)
                addServiceDocs(builder, tapestryProject, services);
        } catch (ProcessCanceledException canceled) {
            throw canceled;
        } catch (Exception ex) {
            _logger.warn("Failed to discover services for library module " + moduleClass, ex);
        }
        services.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getId(), b.getId()));
        return services;
    }

    private static void addServiceDocs(IJavaClassType builder, TapestryProject tapestryProject,
                                       List<Home.ServiceDoc> out) {
        for (Service service : new ModuleBuilder(builder, tapestryProject).getServices()) {
            IJavaClassType serviceClass = service.getServiceClass();
            out.add(new Home.ServiceDoc(
                    service.getId(),
                    serviceClass == null ? "" : serviceClass.getFullyQualifiedName(),
                    service.getScope(),
                    service.isEagerLoad(),
                    serviceClass == null ? "" : serviceClass.getDocumentation()));
        }
    }

    // ---- Rendering ----------------------------------------------------------

    /** Runs an HTML supplier off the EDT (PSI/index access), then renders the result on the EDT. */
    private void renderAsync(Callable<String> supplier) {
        ReadAction.nonBlocking(() -> {
                    try {
                        return supplier.call();
                    } catch (ProcessCanceledException canceled) {
                        throw canceled;
                    } catch (Exception ex) {
                        _logger.warn("Failed to render documentation", ex);
                        return null;
                    }
                })
                .expireWith(_htmlPanel)
                .finishOnUiThread(ModalityState.any(), this::render)
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    /**
     * Renders HTML into the panel, stamping the current IDE theme (JCEF doesn't reliably map
     * prefers-color-scheme to the IDE's dark/light mode) and injecting the navigation bridge.
     */
    private void render(String html) {
        _backButton.setEnabled(!_history.isEmpty());
        if (html == null) {
            clear();
            return;
        }
        String themeClass = JBColor.isBright() ? "light" : "dark";
        String out = html
                .replace("<body", "<body class=\"" + themeClass + "\"")
                .replace("<div class=\"page\">", "<div class=\"page\">" + _breadcrumbHtml)
                .replace("</body>", _bridgeScript + "</body>");
        _htmlPanel.setHtml(out);
    }

    protected void clear() {
        _htmlPanel.setHtml("");
    }

    // ---- Breadcrumbs --------------------------------------------------------

    private static String[] seg(String label, String token) {
        return new String[]{label, token};
    }

    /** Builds the breadcrumb bar; the last segment is the current (non-clickable) page. */
    private void setCrumbs(String[]... segments) {
        StringBuilder sb = new StringBuilder("<nav class=\"crumbs\">");
        sb.append("<img class=\"crumb-logo\" src=\"")
                .append(com.intellij.tapestry.core.model.externalizable.documentation.generationchain.AbstractDocumentationGenerator.logo())
                .append("\" alt=\"Tapestry\">");
        for (int i = 0; i < segments.length; i++) {
            if (i > 0)
                sb.append("<span class=\"sep\">/</span>");

            String label = escape(segments[i][0]);
            String token = segments[i][1];
            boolean last = i == segments.length - 1;

            if (last || token == null || token.isEmpty())
                sb.append("<span class=\"current\">").append(label).append("</span>");
            else
                sb.append("<a href=\"#\" onclick=\"tapestryNav('").append(token).append("');return false;\">")
                        .append(label).append("</a>");
        }
        _breadcrumbHtml = sb.append("</nav>").toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static List<String> toList(Iterable<String> names) {
        List<String> list = new ArrayList<>();
        for (String name : names)
            list.add(name);
        return list;
    }
}
