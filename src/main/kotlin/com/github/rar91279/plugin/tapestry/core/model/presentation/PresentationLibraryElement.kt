package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.exceptions.NotTapestryElementException
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToClass
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToDocumentation
import com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain.DocumentationGenerator
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ClassExternalizer
import com.github.rar91279.plugin.tapestry.core.resource.CoreXmlRecursiveElementVisitor
import com.github.rar91279.plugin.tapestry.core.resource.IResource
import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.util.ComponentUtils
import com.github.rar91279.plugin.tapestry.core.util.LocalizationUtils
import com.github.rar91279.plugin.tapestry.core.util.PathUtils
import com.github.rar91279.plugin.tapestry.intellij.lang.descriptor.TapestryXmlExtension

/**
 * Base class for every presentation element that can be in a Tapestry library.
 */
abstract class PresentationLibraryElement internal constructor(
    val library: TapestryLibrary?,
    val elementClass: IJavaClassType,
    val project: TapestryProject
) : ExternalizableToDocumentation, ExternalizableToClass {

    /** The element type. */
    enum class ElementType { PAGE, COMPONENT, MIXIN }

    private val elementName: String?
    val elementType: ElementType

    private var documentationCache: String? = null
    private var documentationTimestamp: Long = 0
    private var messageCatalogCache: Array<IResource>? = null

    init {
        if (library?.id != null) {
            elementName = getElementNameFromClass(library.basePackage)
            elementType = getElementType(elementClass, library.basePackage)
        } else {
            elementName = try {
                getElementNameFromClass(null)
            } catch (e: NotTapestryElementException) {
                null
            }
            elementType = ElementType.COMPONENT
        }
    }

    open val name: String?
        get() = elementName

    /**
     * @return `true` if this element allows a template, `false` otherwise.
     */
    abstract fun allowsTemplate(): Boolean

    /**
     * The templates associated with this element. An element can have more than one localized
     * template, so this is an array; it is never `null`.
     */
    abstract val template: Array<IResource>

    /** This element's own templates, or the closest super class ones if it declares none. */
    val templateConsiderSuperClass: Array<IResource>
        get() {
            val resources = template
            if (resources.isNotEmpty()) return resources

            val superClass = elementClass.superClassType ?: return emptyArray()
            val superElement = project.findElement(superClass) ?: return emptyArray()

            return superElement.templateConsiderSuperClass
        }

    /**
     * The message catalogs associated with this element, one per localization. Never `null`.
     */
    open val messageCatalog: Array<IResource>
        get() {
            messageCatalogCache?.let { if (checkAllValidResources(it)) return it }

            val fqn = elementClass.fullyQualifiedName!!
            val packageName = fqn.substring(0, fqn.lastIndexOf('.'))
            val catalogName = PathUtils.getLastPathElement(name) + TapestryConstants.PROPERTIES_FILE_EXTENSION

            // Search in the classpath
            val resources = project.resourceFinder.findLocalizedClasspathResource(
                PathUtils.packageIntoPath(packageName, true) + catalogName, true
            )

            return resources
                .filter { LocalizationUtils.unlocalizeFileName(it.name) == catalogName }
                .toTypedArray()
                .also { messageCatalogCache = it }
        }

    /**
     * The element documentation.
     */
    open val description: String?
        get() {
            val lastModified = elementClass.file?.file?.lastModified() ?: 0
            if (documentationCache != null && lastModified <= documentationTimestamp) {
                return documentationCache
            }

            documentationCache = elementClass.documentation
            documentationTimestamp = lastModified

            return documentationCache
        }

    override fun equals(other: Any?): Boolean = other is PresentationLibraryElement && name == other.name

    override fun hashCode(): Int = name?.hashCode() ?: 0

    /**
     * All declared embedded components. Components declared both in the class and in a template are
     * reported by [embeddedComponentsTemplate] instead.
     */
    val embeddedComponents: List<TemplateElement>
        get() {
            val fromClass = ArrayList<TemplateElement>()

            for (field in elementClass.getFields(true).values) {
                if (!field.isValid || field.type !is IJavaClassType) continue
                val annotation = field.annotations[TapestryConstants.COMPONENT_ANNOTATION] ?: continue

                val type = annotation.parameters["type"]?.get(0)
                val component = if (type != null) project.findComponent(type)
                else project.findComponent(field.type as IJavaClassType)

                fromClass.add(TemplateElement(InjectedElement(field, component), "class"))
            }

            val result = ArrayList(fromClass)

            for (resource in template) {
                resource.accept(object : CoreXmlRecursiveElementVisitor {
                    override fun visitTag(tag: XmlTag) {
                        if (fromClass.isEmpty()) return

                        val injected = injectedElementOf(tag) ?: return
                        val injectedId = injected.elementId ?: return

                        for (element in fromClass) {
                            val elementId = element.element?.elementId ?: continue
                            val libraryElement = element.element?.element ?: continue

                            if (elementId.equals(injectedId, ignoreCase = true) &&
                                libraryElement.name.equals(injected.tag?.localName, ignoreCase = true) &&
                                injected.parameters.size != 1
                            ) {
                                result.remove(element)
                            }
                        }
                    }
                })
            }

            return result
        }

    /**
     * All component declarations found in this element's templates.
     */
    val embeddedComponentsTemplate: List<TemplateElement>
        get() {
            val fromTemplate = ArrayList<TemplateElement>()
            val fromClass = embeddedComponents

            for (resource in template) {
                val resourceName = resource.name

                resource.accept(object : CoreXmlRecursiveElementVisitor {
                    override fun visitTag(tag: XmlTag) {
                        val injected = injectedElementOf(tag) ?: return

                        if (fromClass.isEmpty() || !fromClass.contains(TemplateElement(injected, "class"))) {
                            fromTemplate.add(TemplateElement(injected, resourceName))
                        }
                    }
                })
            }

            return fromTemplate
        }

    /**
     * Builds the injected element a component tag stands for, `null` if the tag doesn't resolve to a component.
     */
    private fun injectedElementOf(tag: XmlTag): InjectedElement? {
        if (!ComponentUtils.isComponentTag(tag)) return null

        val typeAttribute = tag.attributes.firstOrNull {
            it.localName == "type" && TapestryXmlExtension.isTapestryTemplateNamespace(it.namespace)
        }

        val component = typeAttribute?.value?.let { project.findComponent(it) }
            ?: project.findComponent(tag.localName ?: return null)
            ?: return null

        return InjectedElement(tag, component)
    }

    /**
     * All pages injected with `@InjectPage`.
     */
    val injectedPages: List<InjectedElement>
        get() {
            val injectedPages = ArrayList<InjectedElement>()

            for (field in elementClass.getFields(true).values) {
                if (!field.isValid) continue
                val annotation = field.annotations[TapestryConstants.INJECT_PAGE_ANNOTATION] ?: continue

                val pageName = annotation.parameters["value"]?.get(0)
                if (pageName != null) {
                    project.findPage(pageName)?.let { injectedPages.add(InjectedElement(field, it)) }
                } else {
                    (field.type as? IJavaClassType)?.let { injectedPages.add(InjectedElement(field, project.findPage(it))) }
                }
            }

            return injectedPages
        }

    override val documentation: String?
        get() = DocumentationGenerator.generate(this)

    @Throws(Exception::class)
    override fun getClassRepresentation(targetClass: IJavaClassType): String? =
        ClassExternalizer.externalize(this, targetClass)

    /**
     * Constructs the element name from its class and library root package.
     *
     * @throws NotTapestryElementException if this is not a Tapestry element.
     */
    protected open fun getElementNameFromClass(libraryRootPackage: String?): String {
        val fqn = elementClass.fullyQualifiedName

        if (!elementClass.isPublic || !elementClass.hasDefaultConstructor()) {
            throw NotTapestryElementException("$fqn is not a valid Tapestry class.")
        }

        if (libraryRootPackage == null) {
            throw NotTapestryElementException("$fqn is not a valid Tapestry class.")
        }

        val elementName = fqn!!.substring(libraryRootPackage.length + 1)

        val elementPackage = ELEMENT_PACKAGES.firstOrNull { elementName.startsWith(it) }
            ?: throw NotTapestryElementException("$fqn is not under a Tapestry base package.")

        return PathUtils.packageIntoPath(elementName.substring(elementPackage.length + 1), false)
    }

    companion object {

        internal const val PARAMETER_ANNOTATION = "org.apache.tapestry5.annotations.Parameter"

        private val ELEMENT_PACKAGES = listOf(
            TapestryConstants.COMPONENTS_PACKAGE,
            TapestryConstants.BASE_PACKAGE,
            TapestryConstants.PAGES_PACKAGE,
            TapestryConstants.MIXINS_PACKAGE
        )

        /**
         * Creates an instance of a presentation element.
         *
         * @throws NotTapestryElementException if the given parameters do not correspond to a Tapestry element.
         */
        fun createElementInstance(
            library: TapestryLibrary,
            elementClass: IJavaClassType,
            project: TapestryProject
        ): PresentationLibraryElement = when (getElementType(elementClass, library.basePackage)) {
            ElementType.COMPONENT -> TapestryComponent(library, elementClass, project)
            ElementType.PAGE -> Page(library, elementClass, project)
            ElementType.MIXIN -> Mixin(library, elementClass, project)
        }

        /**
         * Creates an instance of a presentation element of the current project library.
         *
         * @throws NotTapestryElementException if the given parameters do not correspond to a Tapestry element.
         */
        fun createProjectElementInstance(
            elementClass: IJavaClassType,
            project: TapestryProject?
        ): PresentationLibraryElement? {
            val library = project?.applicationLibrary ?: return null
            return createElementInstance(library, elementClass, project)
        }

        /**
         * Checks if the files in a group of resources are all valid.
         */
        fun checkAllValidResources(resources: Array<IResource>): Boolean =
            resources.all { it.file?.exists() == true }

        private fun getElementType(elementClass: IJavaClassType, basePackage: String?): ElementType {
            val fqn = elementClass.fullyQualifiedName

            val elementName = try {
                fqn!!.substring(basePackage!!.length + 1)
            } catch (ex: IndexOutOfBoundsException) {
                throw NotTapestryElementException("$fqn is not under a Tapestry base package.")
            }

            return when {
                elementName.startsWith(TapestryConstants.COMPONENTS_PACKAGE) ||
                        elementName.startsWith(TapestryConstants.BASE_PACKAGE) -> ElementType.COMPONENT

                elementName.startsWith(TapestryConstants.PAGES_PACKAGE) -> ElementType.PAGE
                elementName.startsWith(TapestryConstants.MIXINS_PACKAGE) -> ElementType.MIXIN
                else -> throw NotTapestryElementException("$fqn is not under a Tapestry base package.")
            }
        }
    }
}
