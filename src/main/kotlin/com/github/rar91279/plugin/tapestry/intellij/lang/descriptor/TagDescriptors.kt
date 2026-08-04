package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.meta.PsiWritableMetaData
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaField
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.intellij.util.ArrayUtil
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlElementDescriptor
import com.intellij.xml.XmlElementsGroup
import com.intellij.xml.XmlNSDescriptor
import com.intellij.xml.util.XmlUtil

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

/** Descriptor of a tag that maps to a Tapestry component. */
class TapestryTagDescriptor(
    private val component: PresentationLibraryElement,
    private val mixins: List<Mixin>,
    namespacePrefix: String?,
    descriptor: TapestryNamespaceDescriptor?
) : BasicTapestryTagDescriptor(namespacePrefix, descriptor) {

    constructor(component: PresentationLibraryElement, prefix: String?, descriptor: TapestryNamespaceDescriptor?) :
        this(component, emptyList(), prefix, descriptor)

    override fun getDefaultName(): String {
        val name = StringUtil.toLowerCase(component.name.orEmpty()).replace('/', '.')
        val shortName = component.library?.shortName

        return getPrefixWithColon() + if (shortName != null) "$shortName.$name" else name
    }

    override fun getAttributesDescriptors(context: XmlTag?): Array<XmlAttributeDescriptor> =
        if (context != null) DescriptorUtil.getAttributeDescriptors(context)
        else {
            val result = ArrayList<XmlAttributeDescriptor>()
            result.addAll(DescriptorUtil.getAttributeDescriptors(component as? TapestryComponent, null))
            for (mixin in mixins) {
                result.addAll(DescriptorUtil.getAttributeDescriptors(mixin, null))
            }
            result.toTypedArray()
        }

    override fun getAttributeDescriptor(attributeName: String, context: XmlTag?): XmlAttributeDescriptor? =
        if (context != null) DescriptorUtil.getAttributeDescriptor(attributeName, context)
        else DescriptorUtil.getAttributeDescriptor(attributeName, component as? TapestryComponent, mixins)

    override fun getDeclaration(): PsiElement? = (component.elementClass as? IntellijJavaClassType)?.psiClass
}

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

/** Descriptor of a `p:`-namespaced tag that passes a block to a component parameter. */
class TapestryParameterDescriptor(
    private val component: TapestryComponent?,
    private val parameter: TapestryParameter,
    namespacePrefix: String?,
    descriptor: TapestryNamespaceDescriptor?
) : BasicTapestryTagDescriptor(namespacePrefix, descriptor) {

    override fun getDefaultName(): String = getPrefixWithColon() + parameter.name

    override fun getAttributesDescriptors(context: XmlTag?): Array<XmlAttributeDescriptor> = XmlAttributeDescriptor.EMPTY

    override fun getAttributeDescriptor(attributeName: String, context: XmlTag?): XmlAttributeDescriptor? = null

    override fun getDeclaration(): PsiElement? {
        // the class field name may be different from the tag name
        (parameter.parameterField as? IntellijJavaField)?.let { return it.psiField }

        val psiClass = (component?.elementClass as? IntellijJavaClassType)?.psiClass ?: return null
        return psiClass.findFieldByName(parameter.name, true)
    }
}

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
