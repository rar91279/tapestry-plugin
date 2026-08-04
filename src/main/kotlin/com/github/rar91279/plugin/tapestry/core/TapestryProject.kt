package com.github.rar91279.plugin.tapestry.core

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex
import com.intellij.psi.impl.java.stubs.index.JavaMethodNameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiModificationTracker
import com.github.rar91279.plugin.tapestry.core.events.TapestryEventsManager
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaTypeCreator
import com.github.rar91279.plugin.tapestry.core.java.IJavaTypeFinder
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.resource.IResourceFinder
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.Page
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.components.BlockComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.components.BodyComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.components.ContainerComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.components.ParameterComponent
import com.github.rar91279.plugin.tapestry.core.util.LocalizationUtils
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.facet.TapestryFacet
import com.github.rar91279.plugin.tapestry.intellij.util.CachedUserDataCache
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import java.io.File

/**
 * A Tapestry project. Every IDE implementation must hold a reference to an instance of this class for each project.
 */
class TapestryProject(
    private val module: Module,
    val resourceFinder: IResourceFinder,
    val javaTypeFinder: IJavaTypeFinder,
    val javaTypeCreator: IJavaTypeCreator
) {

    val eventsManager: TapestryEventsManager = TapestryEventsManager()

    private val coreLibrary = TapestryLibrary(CORE_LIBRARY_ID, TapestryConstants.CORE_LIBRARY_PACKAGE, this)
    private var cachedLibraries: Collection<TapestryLibrary>? = null
    private var cachedLibraryMapping: Map<String, List<String>>? = null

    @Volatile
    private var lastApplicationPackage: String? = null
    private var lastApplicationFilterName: String? = null

    /** The application root package. */
    val applicationRootPackage: String?
        get() {
            val facetPackage = TapestryFacet.findFacetConfiguration(module)?.applicationPackage
            if (StringUtil.isNotEmpty(facetPackage)) return facetPackage

            // No facet (or empty package): fall back to the root package declared via Tapestry-Module-Classes.
            return TapestryUtils.getModuleClassesRootPackage(module)
        }

    /** The application filter name. */
    val applicationFilterName: String?
        get() = TapestryFacet.findFacetConfiguration(module)?.filterName

    /** The application pages root package. */
    val pagesRootPackage: String?
        get() = elementsRootPackage(TapestryConstants.PAGES_PACKAGE)

    /** The application components root package. */
    val componentsRootPackage: String?
        get() = elementsRootPackage(TapestryConstants.COMPONENTS_PACKAGE)

    /** The application mixins root package. */
    val mixinsRootPackage: String?
        get() = elementsRootPackage(TapestryConstants.MIXINS_PACKAGE)

    private fun elementsRootPackage(subpackage: String): String? =
        applicationRootPackage?.let { "$it.$subpackage" }

    /**
     * All the libraries available to this project.
     */
    val libraries: Collection<TapestryLibrary>
        get() {
            val applicationRootPackage = applicationRootPackage ?: return emptyList()
            val applicationFilterName = applicationFilterName
            val libraryMapping = findLibraryMapping()

            // volatile read
            val lastPackage = lastApplicationPackage
            if (StringUtil.isNotEmpty(lastPackage) && StringUtil.isNotEmpty(lastApplicationFilterName)) {
                cachedLibraries?.let {
                    if (lastPackage == applicationRootPackage &&
                        lastApplicationFilterName == applicationFilterName &&
                        libraryMapping == cachedLibraryMapping
                    ) {
                        return it
                    }
                }
            }

            val libraries = ArrayList<TapestryLibrary>()
            libraries.add(TapestryLibrary(APPLICATION_LIBRARY_ID, applicationRootPackage, this))
            libraries.add(TapestryLibrary(APPLICATION_LIBRARY_ID, "$applicationRootPackage.$applicationFilterName", this))
            libraries.add(coreLibrary)

            for ((libraryShortName, basePackages) in libraryMapping) {
                for (basePackage in basePackages) {
                    val shortName = if (libraryShortName == CORE_LIBRARY_ID) null else libraryShortName
                    libraries.add(TapestryLibrary(APPLICATION_LIBRARY_ID, basePackage, this, shortName))
                }
            }

            cachedLibraries = libraries
            cachedLibraryMapping = libraryMapping
            lastApplicationFilterName = applicationFilterName
            lastApplicationPackage = applicationRootPackage // volatile write

            return libraries
        }

    /** The application library. */
    val applicationLibrary: TapestryLibrary?
        get() = libraries.firstOrNull()

    /**
     * @return the page with the given name, or `null` if the page isn't found.
     */
    fun findPage(pageName: String?): Page? =
        ourNameToPageMap.get(module)[StringUtil.toLowerCase(pageName)] as Page?

    /**
     * @return the page of the given class, or `null` if the page isn't found.
     */
    fun findPage(pageClass: IJavaClassType): Page? =
        ourFqnToPageMap.get(module)[pageClass.fullyQualifiedName] as Page?

    val availablePageNames: Array<String>
        get() = ourNameToPageMap.get(module).keys.toTypedArray()

    /**
     * @return the component with the given name, or `null` if the component isn't found.
     */
    fun findComponent(componentName: String): TapestryComponent? =
        // Templates separate subpackages with '.', but element names are stored with '/'.
        ourNameToComponentMap.get(module)[StringUtil.toLowerCase(componentName).replace('.', '/')] as TapestryComponent?

    /**
     * @return the component of the given class, or `null` if the component isn't found.
     */
    fun findComponent(componentClass: IJavaClassType): TapestryComponent? =
        ourFqnToComponentMap.get(module)[componentClass.fullyQualifiedName] as TapestryComponent?

    /**
     * @return the mixin with the given name, or `null` if the mixin isn't found.
     */
    fun findMixin(mixinName: String?): Mixin? =
        ourNameToMixinMap.get(module)[StringUtil.toLowerCase(mixinName).replace('.', '/')] as Mixin?

    val availableComponentNames: Array<String>
        get() = ourNameToComponentMap.get(module).keys.toTypedArray()

    val builtinComponents: Collection<PresentationLibraryElement>
        get() = listOfNotNull(
            BodyComponent.getInstance(this),
            BlockComponent.getInstance(this),
            ParameterComponent.getInstance(this),
            ContainerComponent.getInstance(this)
        )

    val builtinPages: Collection<PresentationLibraryElement>
        get() = emptyList()

    /**
     * Finds a Tapestry element, either a component or a page.
     *
     * @return either the page or component the given class belongs to, or `null` if the element isn't found.
     */
    fun findElement(elementClass: IJavaClassType): PresentationLibraryElement? =
        findComponent(elementClass) ?: findPage(elementClass)

    /**
     * @return the element the given template belongs to, or `null` if it isn't found.
     */
    fun findElementByTemplate(template: PsiFile): PresentationLibraryElement? {
        val templatePath = File(template.originalFile.viewProvider.virtualFile.path).absolutePath
        return ourTemplateToElementMap.get(module)[LocalizationUtils.unlocalizeFileName(templatePath)]
    }

    val availableElements: Collection<PresentationLibraryElement>
        get() = ourFqnToComponentMap.get(module).values

    /** How a referencing element uses the target: declared in a template, or injected in Java code. */
    enum class UsageKind { TEMPLATE, INJECTED }

    /** A single "used by" entry: the referencing element and how it references the target. */
    data class Usage(val user: PresentationLibraryElement, val kind: UsageKind)

    /**
     * Finds the project elements (pages/components) that embed or inject the given element.
     */
    fun findUsages(element: PresentationLibraryElement): List<Usage> =
        ourUsagesMap.get(module)[element.elementClass.fullyQualifiedName] ?: emptyList()

    private fun findLibraryMapping(): Map<String, List<String>> {
        val result = HashMap<String, MutableList<String>>()

        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        for (psiMethod in JavaMethodNameIndex.getInstance().get("contributeComponentClassResolver", module.project, scope)) {
            result.addMappingData(MappingDataCache.getMappingData(psiMethod.containingFile))
        }

        // method annotated with @Contribute(ComponentClassResolver.class)
        for (annotation in JavaAnnotationIndex.getInstance().get("Contribute", module.project, scope)) {
            val attributes = annotation.parameterList.attributes
            if (attributes.size != 1) continue

            val value = attributes[0].value
            if (value is PsiClassObjectAccessExpression && value.operand.text == "ComponentClassResolver") {
                result.addMappingData(MappingDataCache.getMappingData(annotation.containingFile))
            }
        }

        return result
    }

    private fun MutableMap<String, MutableList<String>>.addMappingData(mappingData: Map<String, String>) {
        for ((key, value) in mappingData) {
            computeIfAbsent(key) { ArrayList(2) }.add(value)
        }
    }

    companion object {
        @JvmField
        val JAVA_STRUCTURE_DEPENDENCY: Array<Any> = arrayOf(PsiModificationTracker.MODIFICATION_COUNT)

        @JvmField
        val OUT_OF_CODE_BLOCK_DEPENDENCY: Array<Any> = arrayOf(PsiModificationTracker.MODIFICATION_COUNT)

        /** The application library id. */
        const val APPLICATION_LIBRARY_ID: String = "application"

        /** The Tapestry core library id. */
        const val CORE_LIBRARY_ID: String = "core"

        private val ourNameToPageMap = ElementsCachedMap("ourNameToPageMap", pages = true) {
            StringUtil.toLowerCase(it.name)
        }

        private val ourFqnToPageMap = ElementsCachedMap("ourFqnToPageMap", pages = true) {
            it.elementClass.fullyQualifiedName
        }

        private val ourNameToComponentMap = ElementsCachedMap("ourNameToComponentMap", components = true) {
            StringUtil.toLowerCase(it.name)
        }

        private val ourFqnToComponentMap = ElementsCachedMap("ourFqnToComponentMap", components = true) {
            it.elementClass.fullyQualifiedName
        }

        private val ourNameToMixinMap = ElementsCachedMap("ourNameToMixinMap", mixins = true) {
            StringUtil.toLowerCase(it.name)
        }

        private val ourTemplateToElementMap = ElementsCachedMap(
            "ourTemplateToElementMap", components = true, pages = true, abstractComponents = true
        ) { element ->
            element.template.firstOrNull()?.file?.absolutePath?.let { LocalizationUtils.unlocalizeFileName(it) }
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
                    if (user.elementClass.file == null) continue

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

        private fun MutableMap<String, MutableList<Usage>>.addUsage(
            target: PresentationLibraryElement?,
            user: PresentationLibraryElement,
            kind: UsageKind
        ) {
            val fqn = target?.elementClass?.fullyQualifiedName ?: return
            computeIfAbsent(fqn) { ArrayList() }.add(Usage(user, kind))
        }
    }
}

/**
 * Caches the presentation elements of a module's libraries, keyed by [computeKey].
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
        if (pages) map.putAll(project.builtinPages, null)

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
            put(if (StringUtil.isEmpty(libraryShortName)) key else "$libraryShortName/$key", element)
        }
    }
}
