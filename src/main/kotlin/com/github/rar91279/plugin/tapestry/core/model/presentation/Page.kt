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
