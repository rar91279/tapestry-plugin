package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.javaee.ExternalResourceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.xml.XmlSchemaProvider
import com.intellij.xml.util.XmlUtil

/** Provides the schemas and namespaces available in a Tapestry template. */
class TapestrySchemaProvider : XmlSchemaProvider(), DumbAware {

    override fun getSchema(url: String, module: Module?, baseFile: PsiFile): XmlFile? {
        val location = ExternalResourceManager.getInstance().getResourceLocation(url, baseFile.project)

        return XmlUtil.findXmlFile(baseFile, location)
    }

    override fun isAvailable(file: XmlFile): Boolean = file.fileType is TmlFileType

    override fun getAvailableNamespaces(file: XmlFile, tagName: String?): Set<String> =
        TapestryXmlExtension.tapestryTemplateNamespaces().toMutableSet().apply {
            add(TapestryConstants.PARAMETERS_NAMESPACE)
            add(XmlUtil.XHTML_URI)
        }

    override fun getDefaultPrefix(namespace: String, context: XmlFile): String? = when {
        namespace == XmlUtil.XHTML_URI -> ""
        TapestryXmlExtension.isTapestryTemplateNamespace(namespace) -> "t"
        namespace == TapestryConstants.PARAMETERS_NAMESPACE -> "p"
        else -> null
    }

    override fun getLocations(namespace: String, context: XmlFile): Set<String>? = null
}
