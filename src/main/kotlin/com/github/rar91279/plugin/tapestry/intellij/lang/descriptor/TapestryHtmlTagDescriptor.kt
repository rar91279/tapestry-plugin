package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.intellij.psi.meta.PsiWritableMetaData
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.intellij.util.ArrayUtil
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlElementDescriptor
import com.intellij.xml.XmlElementsGroup
import com.intellij.xml.XmlNSDescriptor
import com.intellij.xml.util.XmlUtil

/**
 * Descriptor of an HTML tag in a Tapestry template: delegates to the HTML descriptor and adds the
 * Tapestry component parameters when the tag is instrumented.
 */
class TapestryHtmlTagDescriptor(
    private val htmlDelegate: XmlElementDescriptor,
    private val component: TapestryComponent?,
    private val mixins: List<Mixin>,
    private val namespaceDescriptor: TapestryNamespaceDescriptor?
) : XmlElementDescriptor, PsiWritableMetaData {

    override fun getQualifiedName(): String = htmlDelegate.qualifiedName

    override fun getDefaultName(): String = htmlDelegate.defaultName

    override fun getElementsDescriptors(context: XmlTag): Array<XmlElementDescriptor> = ArrayUtil.mergeArrays(
        htmlDelegate.getElementsDescriptors(context),
        DescriptorUtil.getTmlSubelementDescriptors(context, namespaceDescriptor)
    )

    override fun getElementDescriptor(childTag: XmlTag, contextTag: XmlTag?): XmlElementDescriptor? {
        htmlDelegate.getElementDescriptor(childTag, contextTag)?.let { return it }

        if (childTag.namespace == XmlUtil.XHTML_URI) {
            if ((contextTag != null && TapestryUtils.getIdentifyingAttribute(contextTag) != null) || isContentTag(contextTag)) {
                return DescriptorUtil.getHtmlTagDescriptorViaNsDescriptor(childTag)
            }
        }

        return DescriptorUtil.getTmlTagDescriptor(childTag)
    }

    override fun getAttributesDescriptors(context: XmlTag?): Array<XmlAttributeDescriptor> {
        val tapestryAttrs =
            if (context != null) DescriptorUtil.getAttributeDescriptors(context)
            else DescriptorUtil.getAttributeDescriptors(component, null)

        return ArrayUtil.mergeArrays(tapestryAttrs, htmlDelegate.getAttributesDescriptors(context))
    }

    override fun getAttributeDescriptor(attributeName: String, context: XmlTag?): XmlAttributeDescriptor? {
        htmlDelegate.getAttributeDescriptor(attributeName, context)?.let { return it }

        return if (context != null) DescriptorUtil.getAttributeDescriptor(attributeName, context)
        else DescriptorUtil.getAttributeDescriptor(attributeName, component, mixins)
    }

    override fun getAttributeDescriptor(attribute: XmlAttribute): XmlAttributeDescriptor? {
        val ns = attribute.namespace

        return if (attribute.namespacePrefix.isEmpty() ||
                   TapestryXmlExtension.isTapestryTemplateNamespace(ns) ||
                   ns == XmlUtil.XHTML_URI
        ) {
            getAttributeDescriptor(attribute.name, attribute.parent)
        }
        else null
    }

    override fun getNSDescriptor(): XmlNSDescriptor? = htmlDelegate.nsDescriptor

    override fun getTopGroup(): XmlElementsGroup? = null

    override fun getContentType(): Int = htmlDelegate.contentType

    override fun getDefaultValue(): String? = null

    override fun getDeclaration(): PsiElement? =
        if (component != null) (component.elementClass as? IntellijJavaClassType)?.psiClass
        else htmlDelegate.declaration

    override fun getName(context: PsiElement?): String = htmlDelegate.getName(context)

    override fun getName(): String = htmlDelegate.name

    override fun init(element: PsiElement) {}

    override fun setName(name: String) {}

    private companion object {

        fun isContentTag(tag: XmlTag?): Boolean =
            tag != null && tag.localName == "content" && TapestryXmlExtension.isTapestryTemplateNamespace(tag.namespace)
    }
}
