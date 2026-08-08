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
 * A Tapestry component.
 */
open class TapestryComponent : ParameterReceiverElement, ExternalizableToTemplate {

    private var templateCache: Array<PsiFile>? = null

    internal constructor(library: TapestryLibrary?, componentClass: PsiClass, project: TapestryProject) :
            super(library, componentClass, project)

    protected constructor(componentClass: PsiClass, project: TapestryProject) :
            super(null, componentClass, project)

    override fun allowsTemplate(): Boolean = true

    override val template: Array<PsiFile>
        get() {
            templateCache?.let { if (checkAllValidResources(it)) return it }

            val fqn = elementClass?.qualifiedName ?: return emptyArray()
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
