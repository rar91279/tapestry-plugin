package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.intellij.psi.meta.PsiWritableMetaData
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlElementDescriptor
import com.intellij.xml.XmlElementsGroup
import com.intellij.xml.XmlNSDescriptor

/**
 * Base class of the Tapestry tag descriptors.
 */
abstract class BasicTapestryTagDescriptor(
    private val namespacePrefix: String?,
    private val namespaceDescriptor: TapestryNamespaceDescriptor?
) : XmlElementDescriptor, PsiWritableMetaData {

    protected fun getPrefixWithColon(): String = if (!namespacePrefix.isNullOrEmpty()) "$namespacePrefix:" else ""

    override fun getQualifiedName(): String = defaultName

    override fun getElementsDescriptors(context: XmlTag): Array<XmlElementDescriptor> =
        DescriptorUtil.getTmlSubelementDescriptors(context, namespaceDescriptor)

    override fun getElementDescriptor(childTag: XmlTag, contextTag: XmlTag?): XmlElementDescriptor? =
        DescriptorUtil.getTmlOrHtmlTagDescriptor(childTag)

    override fun getAttributeDescriptor(attribute: XmlAttribute): XmlAttributeDescriptor? {
        val prefix = attribute.namespacePrefix

        return if (prefix.isEmpty() || prefix == namespacePrefix) getAttributeDescriptor(attribute.name, attribute.parent)
        else null
    }

    override fun getNSDescriptor(): XmlNSDescriptor? = namespaceDescriptor

    override fun getTopGroup(): XmlElementsGroup? = null

    override fun getContentType(): Int = XmlElementDescriptor.CONTENT_TYPE_ANY

    override fun getDefaultValue(): String? = null

    override fun getDeclaration(): PsiElement? = null

    override fun getName(context: PsiElement?): String = defaultName

    override fun getName(): String = defaultName

    override fun init(element: PsiElement) {}

    override fun setName(name: String) {}
}
