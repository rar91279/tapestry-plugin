package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToTemplate
import com.github.rar91279.plugin.tapestry.core.model.externalizable.TemplateExternalizer
import com.github.rar91279.plugin.tapestry.core.resource.IResource
import com.github.rar91279.plugin.tapestry.core.util.PathUtils

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
