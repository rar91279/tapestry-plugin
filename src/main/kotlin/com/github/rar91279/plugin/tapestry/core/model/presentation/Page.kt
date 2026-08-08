package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.intellij.psi.PsiClass
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToTemplate
import com.github.rar91279.plugin.tapestry.core.model.externalizable.TemplateExternalizer
import com.intellij.psi.PsiFile
import com.github.rar91279.plugin.tapestry.core.util.PathUtils

/**
 * A Tapestry page.
 */
class Page internal constructor(
    library: TapestryLibrary?,
    pageClass: PsiClass,
    project: TapestryProject
) : PresentationLibraryElement(library, pageClass, project), ExternalizableToTemplate {

    private var templateCache: Array<PsiFile>? = null

    override fun allowsTemplate(): Boolean = true

    override val template: Array<PsiFile>
        get() {
            templateCache?.let { if (checkAllValidResources(it)) return it }

            val fqn = elementClass?.qualifiedName ?: return emptyArray()
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
