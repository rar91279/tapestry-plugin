package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.javaee.ExternalResourceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiFile
import com.intellij.psi.filters.position.RootTagFilter
import com.intellij.psi.filters.position.TargetNamespaceFilter
import com.intellij.psi.impl.source.xml.XmlElementDescriptorProvider
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.meta.MetaDataRegistrar
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.github.rar91279.plugin.tapestry.psi.TmlFile
import com.intellij.xml.XmlElementDescriptor
import com.intellij.xml.XmlSchemaProvider
import com.intellij.xml.util.XmlUtil

/** Registers the namespace descriptors of the Tapestry namespaces. */
class TapestryMetaDataContributor : MetaDataContributor {

    override fun contributeMetaData(registrar: MetaDataRegistrar) {
        registrar.registerMetaData(
            RootTagFilter(TargetNamespaceFilter(TapestryXmlExtension.tapestryTemplateNamespaces())),
            TapestryNamespaceDescriptor::class.java
        )
        registrar.registerMetaData(
            RootTagFilter(TargetNamespaceFilter(TapestryConstants.PARAMETERS_NAMESPACE)),
            TapestryParametersNamespaceDescriptor::class.java
        )
    }
}

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

/** Provides the element descriptors of the tags in a Tapestry template. */
class TapestryTagDescriptorProvider : XmlElementDescriptorProvider {

    override fun getDescriptor(tag: XmlTag): XmlElementDescriptor? {
        if (DumbService.isDumb(tag.project)) return null

        return if (tag.containingFile is TmlFile) DescriptorUtil.getTmlOrHtmlTagDescriptor(tag) else null
    }
}
