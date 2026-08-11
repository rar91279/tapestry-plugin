package com.github.rar91279.plugin.tapestry.core

import com.github.rar91279.plugin.tapestry.core.events.TapestryEventsManager
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.Page
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.components.BuiltinComponent
import com.github.rar91279.plugin.tapestry.core.resource.IResourceFinder
import com.github.rar91279.plugin.tapestry.core.util.LocalizationUtils
import com.github.rar91279.plugin.tapestry.core.util.classType
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryFacet
import com.github.rar91279.plugin.tapestry.intellij.util.CachedUserDataCache
import com.github.rar91279.plugin.tapestry.intellij.util.JavaTypeCreator
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex
import com.intellij.psi.impl.java.stubs.index.JavaMethodNameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.util.PsiModificationTracker

/**
 * A Tapestry project. Every IDE implementation must hold a reference to an instance of this class for each project.
 *
 * @param module the IntelliJ IDEA module this Tapestry project is associated with
 * @param resourceFinder the resource finder for locating Tapestry resources
 * @param javaTypeCreator the Java type creator for creating PSI types
 */
class TapestryProject(
    private val module: Module,
    val resourceFinder: IResourceFinder,
    val javaTypeCreator: JavaTypeCreator
) {

    // --- Java types ---------------------------------------------------------------------------------
    // These wrap JavaPsiFacade rather than being a separate `IJavaTypeFinder` collaborator: there is one
    // implementation, and putting them here keeps them mockable from the fast core specs.

    /**
     * Finds a class by its fully qualified name.
     *
     * @param fullyQualifiedName the fully qualified name of the class to find
     * @param includeDependencies whether to include dependencies in the search scope
     * @return the class with the given fully qualified name, `null` if none is found
     */
    fun findType(fullyQualifiedName: String, includeDependencies: Boolean): PsiClass? =
        JavaPsiFacade.getInstance(module.project).findClass(fullyQualifiedName, scope(includeDependencies))

    /**
     * Finds a class type by its fully qualified name.
     *
     * @param fullyQualifiedName the fully qualified name of the class
     * @return the type denoting the class with the given fully qualified name, `null` if not found
     */
    fun findClassType(fullyQualifiedName: String): PsiClassType? =
        findType(fullyQualifiedName, true)?.classType()

    /**
     * Creates a class type for the given PSI class.
     *
     * @param psiClass the PSI class to create a type for
     * @return the type denoting [psiClass]
     */
    fun classTypeOf(psiClass: PsiClass): PsiClassType = psiClass.classType()

    /**
     * Finds all classes declared in a specific package.
     *
     * @param packageName the name of the package to search in
     * @param includeDependencies whether to include dependencies in the search scope
     * @return all the classes declared in the given package
     */
    fun findTypesInPackage(packageName: String, includeDependencies: Boolean): Collection<PsiClass> =
        findPackage(packageName)?.getClasses(scope(includeDependencies))?.toList() ?: emptyList()

    /**
     * Finds all classes declared in a package and its sub-packages recursively.
     *
     * @param basePackageName the base package name to start searching from
     * @param includeDependencies whether to include dependencies in the search scope
     * @return all the classes declared in the given package and its sub-packages
     */
    fun findTypesInPackageRecursively(basePackageName: String, includeDependencies: Boolean): Collection<PsiClass> {
        val psiPackage = findPackage(basePackageName) ?: return emptyList()
        val scope = scope(includeDependencies)

        val types = mutableListOf<PsiClass>()
        types.addAll(psiPackage.getClasses(scope))
        for (subPackage in psiPackage.getSubPackages(scope)) {
            types.addAll(findTypesInPackageRecursively(subPackage.qualifiedName, includeDependencies))
        }

        return types
    }

    /**
     * Finds a package by its name.
     *
     * @param packageName the name of the package to find
     * @return the package with the given name, `null` if not found
     */
    private fun findPackage(packageName: String): PsiPackage? =
        JavaPsiFacade.getInstance(module.project).findPackage(packageName)

    /**
     * Creates a search scope for the module.
     *
     * @param includeDependencies whether to include dependencies and libraries in the scope
     * @return the appropriate search scope for the module
     */
    private fun scope(includeDependencies: Boolean): GlobalSearchScope =
        if (includeDependencies) GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        else GlobalSearchScope.moduleScope(module)

    /**
     * The events manager for this Tapestry project.
     */
    val eventsManager: TapestryEventsManager = TapestryEventsManager()

    /**
     * The Tapestry core library instance.
     */
    private val coreLibrary = TapestryLibrary(CORE_LIBRARY_ID, TapestryConstants.CORE_LIBRARY_PACKAGE, this)

    /**
     * One consistent, immutable view of [libraries] and the inputs it was computed from. Published
     * through a single volatile write so a concurrent reader either sees a complete snapshot or none —
     * the previous four separate fields could publish the guard before the payload and hand a reader a
     * stale or empty library set.
     *
     * @property applicationPackage the application root package
     * @property filterName the application filter name
     * @property modificationCount the PSI modification count when this snapshot was created
     * @property libraries the list of Tapestry libraries
     */
    private class LibrarySnapshot(
        val applicationPackage: String,
        val filterName: String?,
        val modificationCount: Long,
        val libraries: List<TapestryLibrary>,
    )

    /**
     * Cached snapshot of the libraries and their computation inputs, published atomically.
     */
    @Volatile
    private var librarySnapshot: LibrarySnapshot? = null

    /**
     * The application root package.
     *
     * Returns the application root package from the Tapestry facet configuration, or falls back
     * to the root package declared via Tapestry-Module-Classes if no facet is configured.
     */
    val applicationRootPackage: String?
        get() {
            val facetPackage = TapestryFacet.findFacetConfiguration(module)?.applicationPackage
            if (!facetPackage.isNullOrEmpty()) return facetPackage

            // No facet (or empty package): fall back to the root package declared via Tapestry-Module-Classes.
            return TapestryUtils.getModuleClassesRootPackage(module)
        }

    /**
     * The application filter name.
     *
     * Returns the filter name from the Tapestry facet configuration.
     */
    val applicationFilterName: String?
        get() = TapestryFacet.findFacetConfiguration(module)?.filterName

    /**
     * The application pages root package.
     *
     * Returns the full package path for pages (applicationRootPackage.pages).
     */
    val pagesRootPackage: String?
        get() = elementsRootPackage(TapestryConstants.PAGES_PACKAGE)

    /**
     * The application components root package.
     *
     * Returns the full package path for components (applicationRootPackage.components).
     */
    val componentsRootPackage: String?
        get() = elementsRootPackage(TapestryConstants.COMPONENTS_PACKAGE)

    /**
     * The application mixins root package.
     *
     * Returns the full package path for mixins (applicationRootPackage.mixins).
     */
    val mixinsRootPackage: String?
        get() = elementsRootPackage(TapestryConstants.MIXINS_PACKAGE)

    /**
     * Constructs a root package path for a specific element type.
     *
     * @param subpackage the subpackage name (e.g., "pages", "components", "mixins")
     * @return the full package path, or `null` if application root package is not defined
     */
    private fun elementsRootPackage(subpackage: String): String? =
        applicationRootPackage?.let { "$it.$subpackage" }

    /**
     * All the libraries available to this project.
     *
     * Includes the application library, core library, and any additional libraries
     * discovered through library mappings. The result is cached and invalidated
     * on PSI modifications or facet configuration changes.
     */
    val libraries: Collection<TapestryLibrary>
        get() {
            val applicationRootPackage = applicationRootPackage ?: return emptyList()
            val applicationFilterName = applicationFilterName
            // The library mapping is derived purely from PSI (stub indexes + MappingDataCache), so the
            // modification count stands in for it. The two facet fields are not PSI and are compared
            // directly. All three are cheap — unlike findLibraryMapping(), which the previous guard ran
            // on every access just to decide whether it could skip the work it had already done.
            val modificationCount = PsiModificationTracker.getInstance(module.project).modificationCount

            librarySnapshot?.let {
                if (it.applicationPackage == applicationRootPackage &&
                    it.filterName == applicationFilterName &&
                    it.modificationCount == modificationCount
                ) {
                    return it.libraries
                }
            }

            val libraries = buildList {
                add(TapestryLibrary(APPLICATION_LIBRARY_ID, applicationRootPackage, this@TapestryProject))
                add(
                    TapestryLibrary(
                        APPLICATION_LIBRARY_ID,
                        "$applicationRootPackage.$applicationFilterName",
                        this@TapestryProject
                    )
                )
                add(coreLibrary)

                for ((libraryShortName, basePackages) in findLibraryMapping()) {
                    for (basePackage in basePackages) {
                        val shortName = if (libraryShortName == CORE_LIBRARY_ID) null else libraryShortName
                        add(TapestryLibrary(APPLICATION_LIBRARY_ID, basePackage, this@TapestryProject, shortName))
                    }
                }
            }

            // Single volatile publish, after the payload is fully built.
            librarySnapshot =
                LibrarySnapshot(applicationRootPackage, applicationFilterName, modificationCount, libraries)

            return libraries
        }

    /**
     * The application library.
     *
     * Returns the first library in the libraries collection, which is always the application library.
     */
    val applicationLibrary: TapestryLibrary?
        get() = libraries.firstOrNull()

    /**
     * Finds a page by its name.
     *
     * @param pageName the name of the page to find (case-insensitive)
     * @return the page with the given name, or `null` if the page isn't found
     */
    fun findPage(pageName: String?): Page? =
        ourNameToPageMap.get(module)[pageName?.lowercase()] as Page?

    /**
     * Finds a page by its PSI class.
     *
     * @param pageClass the PSI class representing the page
     * @return the page of the given class, or `null` if the page isn't found
     */
    fun findPage(pageClass: PsiClass): Page? =
        ourFqnToPageMap.get(module)[pageClass.qualifiedName] as Page?

    /**
     * All available page names in this project.
     *
     * Returns an array of all page names that can be found in the project.
     */
    val availablePageNames: Array<String>
        get() = ourNameToPageMap.get(module).keys.toTypedArray()

    /**
     * Finds a component by its name.
     *
     * Templates separate subpackages with '.', but element names are stored with '/'.
     * This method handles the conversion automatically.
     *
     * @param componentName the name of the component to find (case-insensitive)
     * @return the component with the given name, or `null` if the component isn't found
     */
    fun findComponent(componentName: String): TapestryComponent? =
        // Templates separate subpackages with '.', but element names are stored with '/'.
        ourNameToComponentMap.get(module)[componentName.lowercase().replace('.', '/')] as TapestryComponent?

    /**
     * Finds a component by its PSI class.
     *
     * @param componentClass the PSI class representing the component
     * @return the component of the given class, or `null` if the component isn't found
     */
    fun findComponent(componentClass: PsiClass): TapestryComponent? =
        ourFqnToComponentMap.get(module)[componentClass.qualifiedName] as TapestryComponent?

    /**
     * Finds a mixin by its name.
     *
     * @param mixinName the name of the mixin to find (case-insensitive)
     * @return the mixin with the given name, or `null` if the mixin isn't found
     */
    fun findMixin(mixinName: String?): Mixin? =
        ourNameToMixinMap.get(module)[mixinName?.lowercase()?.replace('.', '/')] as Mixin?

    /**
     * All available component names in this project.
     *
     * Returns an array of all component names that can be found in the project.
     */
    val availableComponentNames: Array<String>
        get() = ourNameToComponentMap.get(module).keys.toTypedArray()

    /**
     * All built-in Tapestry components.
     *
     * Returns a collection of all core Tapestry components provided by the framework.
     */
    val builtinComponents: Collection<PresentationLibraryElement>
        get() = BuiltinComponent.all(this)

    /**
     * Finds a Tapestry element, either a component or a page.
     *
     * @param elementClass the PSI class to search for
     * @return either the page or component the given class belongs to, or `null` if the element isn't found
     */
    fun findElement(elementClass: PsiClass): PresentationLibraryElement? =
        findComponent(elementClass) ?: findPage(elementClass)

    /**
     * Finds the Tapestry element (page or component) that owns a template file.
     *
     * @param template the PSI file representing the template
     * @return the element the given template belongs to, or `null` if it isn't found
     */
    fun findElementByTemplate(template: PsiFile): PresentationLibraryElement? {
        // Keyed on the VFS path, which `ourTemplateToElementMap` also builds from — both sides must derive
        // the key the same way. Don't route either through java.io.File: it yields platform separators
        // (backslashes on Windows) where VFS paths are always forward-slashed.
        val templatePath = template.originalFile.viewProvider.virtualFile.path
        return ourTemplateToElementMap.get(module)[LocalizationUtils.unlocalizeFileName(templatePath)]
    }

    /**
     * All available Tapestry elements (pages and components) in this project.
     *
     * Returns a collection of all presentation library elements found in the project.
     */
    val availableElements: Collection<PresentationLibraryElement>
        get() = ourFqnToComponentMap.get(module).values

    /**
     * How a referencing element uses the target: declared in a template, or injected in Java code.
     */
    enum class UsageKind {
        /** The element is referenced in a template file. */
        TEMPLATE,

        /** The element is injected in Java/Kotlin code. */
        INJECTED
    }

    /**
     * A single "used by" entry: the referencing element and how it references the target.
     *
     * @property user the element that uses/references the target
     * @property kind how the target is used (in template or injected)
     */
    data class Usage(val user: PresentationLibraryElement, val kind: UsageKind)

    /**
     * Finds the project elements (pages/components) that embed or inject the given element.
     *
     * @param element the element to find usages for
     * @return a list of usages showing which elements reference the given element and how
     */
    fun findUsages(element: PresentationLibraryElement): List<Usage> =
        ourUsagesMap.get(module)[element.elementClass?.qualifiedName] ?: emptyList()

    /**
     * Discovers all Tapestry library mappings in the project.
     *
     * Scans for library mappings declared through LibraryMapping classes,
     * contributeComponentClassResolver methods, and @Contribute annotations.
     *
     * @return a map from library short names to their base package paths
     */
    private fun findLibraryMapping(): Map<String, List<String>> {
        val result = HashMap<String, MutableList<String>>()

        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)

        // Any source file naming LibraryMapping, whatever its language: the two Java indexes below are
        // blind to a Kotlin module class, and they only cover the two contribution shapes they look for.
        PsiSearchHelper.getInstance(module.project).processAllFilesWithWord(
            LIBRARY_MAPPING_CLASS,
            scope,
            { file -> result.addMappingData(MappingDataCache.getMappingData(file)); true },
            true
        )

        // Compiled module classes in library jars, which the word index doesn't reach.
        JavaMethodNameIndex.getInstance().getMethods("contributeComponentClassResolver", module.project, scope)
            .forEach {
                result.addMappingData(MappingDataCache.getMappingData(it.containingFile))
            }


        // method annotated with @Contribute(ComponentClassResolver.class)
        JavaAnnotationIndex.getInstance().getAnnotations("Contribute", module.project, scope).forEach { annotation ->
            val attributes = annotation.parameterList.attributes
            if (attributes.size != 1) return@forEach

            val value = attributes[0].value
            if (value is PsiClassObjectAccessExpression && value.operand.text == "ComponentClassResolver") {
                result.addMappingData(MappingDataCache.getMappingData(annotation.containingFile))
            }
        }

        return result
    }

    /**
     * Adds mapping data to the library mapping result, avoiding duplicates.
     *
     * @receiver the mutable map to add mappings to
     * @param mappingData the mapping data to add (library short name to base package)
     */
    private fun MutableMap<String, MutableList<String>>.addMappingData(mappingData: Map<String, String>) {
        mappingData.forEach { (key, value) ->
            // A file can be reached by both the word sweep and the index lookups — don't map it twice.
            getOrPut(key) { ArrayList(2) }.let { list ->
                if (value !in list) list.add(value)
            }
        }
    }

    companion object {
        /**
         * The dependency array for cached values that should be invalidated on any PSI structure change.
         */
        val JAVA_STRUCTURE_DEPENDENCY: Array<Any> = arrayOf(PsiModificationTracker.MODIFICATION_COUNT)

        /**
         * The application library id.
         */
        const val APPLICATION_LIBRARY_ID: String = "application"

        /**
         * The Tapestry core library id.
         */
        const val CORE_LIBRARY_ID: String = "core"

        /**
         * Short name of the class a library mapping is declared with, as the word index sees it.
         */
        private const val LIBRARY_MAPPING_CLASS: String = "LibraryMapping"

        private val ourNameToPageMap = ElementsCachedMap("ourNameToPageMap", pages = true) {
            it.name?.lowercase()
        }

        private val ourFqnToPageMap = ElementsCachedMap("ourFqnToPageMap", pages = true) {
            it.elementClass?.qualifiedName
        }

        private val ourNameToComponentMap = ElementsCachedMap("ourNameToComponentMap", components = true) {
            it.name?.lowercase()
        }

        private val ourFqnToComponentMap = ElementsCachedMap("ourFqnToComponentMap", components = true) {
            it.elementClass?.qualifiedName
        }

        private val ourNameToMixinMap = ElementsCachedMap("ourNameToMixinMap", mixins = true) {
            it.name?.lowercase()
        }

        private val ourTemplateToElementMap = ElementsCachedMap(
            "ourTemplateToElementMap", components = true, pages = true, abstractComponents = true
        ) { element ->
            element.template.firstOrNull()?.virtualFile?.path?.let { LocalizationUtils.unlocalizeFileName(it) }
        }

        // Reverse-dependency map: target class FQN -> project elements that embed/inject it.
        // Cached per module and invalidated on any PSI change (same dependency as the forward maps).
        // ponytail: full application-library scan on (re)compute; narrow the dependency or index
        // incrementally if it gets slow on very large projects.
        private val ourUsagesMap = object : CachedUserDataCache<Map<String, List<Usage>>, Module>("ourUsagesMap") {

            override fun computeValue(module: Module): Map<String, List<Usage>> {
                val usages = HashMap<String, MutableList<Usage>>()
                val project = TapestryModuleSupportLoader.getTapestryProject(module) ?: return usages
                val application = project.applicationLibrary ?: return usages

                val candidates = ArrayList(application.components.values)
                candidates.addAll(application.pages.values)

                for (user in candidates) {
                    if (user.elementClass == null) continue

                    for (embedded in user.embeddedComponents) {
                        usages.addUsage(embedded.element?.element, user, UsageKind.INJECTED)
                    }
                    for (embedded in user.embeddedComponentsTemplate) {
                        usages.addUsage(embedded.element?.element, user, UsageKind.TEMPLATE)
                    }
                    for (injected in user.injectedPages) {
                        usages.addUsage(injected.element, user, UsageKind.INJECTED)
                    }
                }
                return usages
            }

            override fun getDependencies(module: Module): Array<Any> = JAVA_STRUCTURE_DEPENDENCY

            override fun getProject(module: Module): Project = module.project
        }

        /**
         * Adds a usage entry to the usages map.
         *
         * @receiver the mutable map of usages to add to
         * @param target the element being used/referenced
         * @param user the element that uses/references the target
         * @param kind how the target is used (in template or injected)
         */
        private fun MutableMap<String, MutableList<Usage>>.addUsage(
            target: PresentationLibraryElement?,
            user: PresentationLibraryElement,
            kind: UsageKind
        ) {
            val fqn = target?.elementClass?.qualifiedName ?: return
            computeIfAbsent(fqn) { ArrayList() }.add(Usage(user, kind))
        }
    }
}

/**
 * Caches the presentation elements of a module's libraries, keyed by [computeKey].
 *
 * This cache maintains a mapping from keys (computed by [computeKey]) to presentation elements
 * of various types. The cache is invalidated on PSI structure changes.
 *
 * @param keyName the name of this cache for debugging purposes
 * @param components whether to include components in the cache
 * @param pages whether to include pages in the cache
 * @param mixins whether to include mixins in the cache
 * @param abstractComponents whether to include abstract components in the cache
 * @param computeKey function to compute the map key for each element
 */
private class ElementsCachedMap(
    keyName: String,
    private val components: Boolean = false,
    private val pages: Boolean = false,
    private val mixins: Boolean = false,
    private val abstractComponents: Boolean = false,
    private val computeKey: (PresentationLibraryElement) -> String?
) : CachedUserDataCache<Map<String, PresentationLibraryElement>, Module>(keyName) {

    init {
        require(components || pages || mixins || abstractComponents)
    }

    override fun computeValue(module: Module): Map<String, PresentationLibraryElement> {
        val map = HashMap<String, PresentationLibraryElement>()
        val project = checkNotNull(TapestryModuleSupportLoader.getTapestryProject(module))

        for (library in project.libraries) {
            if (components) map.putAll(library.components.values, library.shortName)
            if (abstractComponents) map.putAll(library.abstractComponents.values, library.shortName)
            if (pages) map.putAll(library.pages.values, library.shortName)
            if (mixins) map.putAll(library.mixins.values, library.shortName)
        }
        if (components) map.putAll(project.builtinComponents, null)

        return map
    }

    override fun getDependencies(module: Module): Array<Any> = TapestryProject.JAVA_STRUCTURE_DEPENDENCY

    override fun getProject(module: Module): Project = module.project

    private fun MutableMap<String, PresentationLibraryElement>.putAll(
        elements: Collection<PresentationLibraryElement>,
        libraryShortName: String?
    ) {
        for (element in elements) {
            val key = computeKey(element) ?: continue
            put(if (libraryShortName.isNullOrEmpty()) key else "$libraryShortName/$key", element)
        }
    }
}
