package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor

/** Descriptor of a Tapestry-namespaced tag that doesn't map to any known component. */
class TapestryUnknownTagDescriptor(
    componentName: String,
    namespacePrefix: String?,
    descriptor: TapestryNamespaceDescriptor?
) : BasicTapestryTagDescriptor(namespacePrefix, descriptor) {

    private val qualifiedName: String = getPrefixWithColon() + StringUtil.toLowerCase(componentName)

    override fun getDefaultName(): String = qualifiedName

    override fun getAttributesDescriptors(context: XmlTag?): Array<XmlAttributeDescriptor> =
        if (context != null) DescriptorUtil.getAttributeDescriptors(context) else XmlAttributeDescriptor.EMPTY

    override fun getAttributeDescriptor(attributeName: String, context: XmlTag?): XmlAttributeDescriptor? =
        if (context != null) DescriptorUtil.getAttributeDescriptor(attributeName, context) else null
}
