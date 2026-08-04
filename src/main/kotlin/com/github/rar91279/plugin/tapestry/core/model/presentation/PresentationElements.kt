package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToTemplate
import com.github.rar91279.plugin.tapestry.core.model.externalizable.TemplateExternalizer
import com.github.rar91279.plugin.tapestry.core.model.presentation.components.DummyTapestryParameter
import com.github.rar91279.plugin.tapestry.core.resource.IResource
import com.github.rar91279.plugin.tapestry.core.util.PathUtils

/**
 * A presentation element that declares Tapestry parameters.
 */
abstract class ParameterReceiverElement internal constructor(
    library: TapestryLibrary?,
    elementClass: IJavaClassType,
    project: TapestryProject
) : PresentationLibraryElement(library, elementClass, project) {

    private var parametersCache: Map<String, TapestryParameter>? = null
    private var parametersCacheTimestamp: Long = 0

    /**
     * The declared Tapestry parameters, by name.
     */
    open val parameters: Map<String, TapestryParameter>
        get() {
            val lastModified = elementClass.file?.file?.lastModified() ?: 0
            parametersCache?.let { if (lastModified <= parametersCacheTimestamp) return it }

            val parameters = HashMap<String, TapestryParameter>()
            parameters["mixins"] = DummyTapestryParameter(project, "mixins", false)
            parametersCacheTimestamp = lastModified

            for (field in elementClass.getFields(true).values) {
                if (field.isPrivate && field.isValid && field.annotations.containsKey(PARAMETER_ANNOTATION)) {
                    val parameter = TapestryParameter(elementClass, field)
                    parameters[parameter.name] = parameter
                }
            }

            return parameters.toMap().also { parametersCache = it }
        }

    /** The declared Tapestry required parameters. */
    val requiredParameters: Map<String, TapestryParameter>
        get() = parameters.filterValues { it.isRequired }

    /** The declared Tapestry not required parameters. */
    val optionalParameters: Map<String, TapestryParameter>
        get() = parameters.filterValues { !it.isRequired }
}

/**
 * A Tapestry component.
 */
open class TapestryComponent : ParameterReceiverElement, ExternalizableToTemplate {

    private var templateCache: Array<IResource>? = null

    internal constructor(library: TapestryLibrary?, componentClass: IJavaClassType, project: TapestryProject) :
            super(library, componentClass, project)

    protected constructor(componentClass: IJavaClassType, project: TapestryProject) :
            super(null, componentClass, project)

    override fun allowsTemplate(): Boolean = true

    override val template: Array<IResource>
        get() {
            templateCache?.let { if (checkAllValidResources(it)) return it }

            val fqn = elementClass.fullyQualifiedName!!
            val packageName = fqn.substring(0, fqn.lastIndexOf('.'))

            // Search in the classpath
            val resources = project.resourceFinder.findLocalizedClasspathResource(
                PathUtils.packageIntoPath(packageName, true) +
                        PathUtils.getLastPathElement(name) + "." + TapestryConstants.TEMPLATE_FILE_EXTENSION,
                true
            )

            return resources.toTypedArray().also { templateCache = it }
        }

    @Throws(Exception::class)
    override fun getTemplateRepresentation(namespacePrefix: String?): String? =
        TemplateExternalizer.externalize(this, namespacePrefix)
}

/**
 * A Tapestry page.
 */
class Page internal constructor(
    library: TapestryLibrary?,
    pageClass: IJavaClassType,
    project: TapestryProject
) : PresentationLibraryElement(library, pageClass, project), ExternalizableToTemplate {

    private var templateCache: Array<IResource>? = null

    override fun allowsTemplate(): Boolean = true

    override val template: Array<IResource>
        get() {
            templateCache?.let { if (checkAllValidResources(it)) return it }

            val fqn = elementClass.fullyQualifiedName!!
            val packageName = fqn.substring(0, fqn.lastIndexOf('.'))
            val templateName = PathUtils.getLastPathElement(name) + "." + TapestryConstants.TEMPLATE_FILE_EXTENSION

            // Search in the classpath, then in the web application context
            val templates = ArrayList(
                project.resourceFinder.findLocalizedClasspathResource(
                    PathUtils.packageIntoPath(packageName, true) + templateName, true
                )
            )
            templates.addAll(project.resourceFinder.findLocalizedContextResource("$name.${TapestryConstants.TEMPLATE_FILE_EXTENSION}"))

            return templates.toTypedArray().also { templateCache = it }
        }

    @Throws(Exception::class)
    override fun getTemplateRepresentation(namespacePrefix: String?): String? =
        TemplateExternalizer.externalize(this, namespacePrefix)
}

/**
 * A Tapestry mixin.
 */
class Mixin internal constructor(
    library: TapestryLibrary?,
    componentClass: IJavaClassType,
    project: TapestryProject
) : ParameterReceiverElement(library, componentClass, project) {

    override fun allowsTemplate(): Boolean = false

    override val template: Array<IResource> get() = emptyArray()

    override val messageCatalog: Array<IResource> get() = emptyArray()
}
