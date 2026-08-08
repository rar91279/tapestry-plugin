package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.psi.TmlFile
import com.intellij.xml.DefaultXmlExtension
import com.intellij.xml.XmlNSDescriptor
import com.intellij.xml.impl.dtd.XmlNSDescriptorImpl
import com.intellij.xml.util.XmlUtil

class TapestryXmlExtension : DefaultXmlExtension() {

    override fun getNamespacesFromDocument(parent: XmlDocument, declarationsExist: Boolean): Array<Array<String>> {
        val namespaces = arrayOf(
            arrayOf("", XmlUtil.XHTML_URI),
            arrayOf("t", TapestryConstants.TEMPLATE_NAMESPACE),
            arrayOf("p", TapestryConstants.PARAMETERS_NAMESPACE)
        )

        val rootTag = parent.rootTag ?: return namespaces

        for (attribute in rootTag.attributes) {
            if (!attribute.isNamespaceDeclaration) continue

            val attributeValue = attribute.value

            when {
                attributeValue == TapestryConstants.PARAMETERS_NAMESPACE ->
                    namespaces[2][0] = namespacePrefixFromDeclaration(attribute)

                isTapestryTemplateNamespace(attributeValue) -> {
                    namespaces[1][0] = namespacePrefixFromDeclaration(attribute)
                    namespaces[1][1] = attributeValue!!
                }

                attributeValue == XmlUtil.XHTML_URI ->
                    namespaces[0][0] = namespacePrefixFromDeclaration(attribute)
            }
        }

        return namespaces
    }

    override fun isAvailable(file: PsiFile?): Boolean = file is TmlFile

    override fun isRequiredAttributeImplicitlyPresent(tag: XmlTag, attrName: String): Boolean {
        val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(tag)
            ?: return super.isRequiredAttributeImplicitlyPresent(tag, attrName)

        if (tag.getAttribute(attrName, getTapestryNamespace(tag)) != null) return true

        val element = tapestryProject.findElementByTemplate(tag.containingFile) ?: return false
        val elementClass = element.elementClass ?: return false

        return TapestryUtils.parameterDefinedInClass(attrName, elementClass, tag)
    }

    override fun getNSDescriptor(element: XmlTag, namespace: String, strict: Boolean): XmlNSDescriptor? {
        if (element.containingFile !is TmlFile) return null

        return element.getNSDescriptor(namespace, strict)
    }

    override fun getDescriptorFromDoctype(file: XmlFile, descriptor: XmlNSDescriptor?): XmlNSDescriptor? {
        if (file is TmlFile && descriptor is XmlNSDescriptorImpl) {
            if (file.document?.prolog?.doctype != null) return DescriptorUtil.getHtmlNSDescriptor(file)
        }

        return descriptor
    }

    private fun namespacePrefixFromDeclaration(attribute: XmlAttribute): String =
        if (attribute.localName == attribute.name) "" else attribute.localName

    companion object {

        private val TEMPLATE_NAMESPACES = setOf(
            TapestryConstants.TEMPLATE_NAMESPACE,
            TapestryConstants.TEMPLATE_NAMESPACE2,
            TapestryConstants.TEMPLATE_NAMESPACE3,
            TapestryConstants.TEMPLATE_NAMESPACE4
        )

        fun isTapestryTemplateNamespace(namespace: String?): Boolean = namespace in TEMPLATE_NAMESPACES

        fun getTapestryNamespace(tag: XmlTag?): String =
            TEMPLATE_NAMESPACES.firstOrNull { tag?.getPrefixByNamespace(it) != null } ?: TapestryConstants.TEMPLATE_NAMESPACE

        fun getTapestryTemplateDescriptor(tag: XmlTag): TapestryNamespaceDescriptor? =
            tag.getNSDescriptor(getTapestryNamespace(tag), true) as? TapestryNamespaceDescriptor

        fun tapestryTemplateNamespaces(): Array<String> = TEMPLATE_NAMESPACES.toTypedArray()
    }
}
