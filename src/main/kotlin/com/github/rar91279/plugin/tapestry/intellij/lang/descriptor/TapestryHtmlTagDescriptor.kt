package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.intellij.psi.meta.PsiWritableMetaData
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
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
 *
 * @param htmlDelegate the underlying HTML element descriptor to delegate standard HTML functionality to
 * @param component the Tapestry component associated with this tag, or null if not instrumented
 * @param mixins the list of mixins applied to this component
 * @param namespaceDescriptor the Tapestry namespace descriptor for resolving TML elements
 */
class TapestryHtmlTagDescriptor(
    private val htmlDelegate: XmlElementDescriptor,
    private val component: TapestryComponent?,
    private val mixins: List<Mixin>,
    private val namespaceDescriptor: TapestryNamespaceDescriptor?
) : XmlElementDescriptor, PsiWritableMetaData {

    /**
     * Returns the qualified name of this tag descriptor.
     *
     * @return the qualified name delegated from the HTML descriptor
     */
    override fun getQualifiedName(): String = htmlDelegate.qualifiedName

    /**
     * Returns the default name of this tag descriptor.
     *
     * @return the default name delegated from the HTML descriptor
     */
    override fun getDefaultName(): String = htmlDelegate.defaultName

    /**
     * Returns all possible child element descriptors for the given context tag.
     * Merges HTML element descriptors with Tapestry TML subelement descriptors.
     *
     * @param context the XML tag context
     * @return array of child element descriptors combining HTML and TML elements
     */
    override fun getElementsDescriptors(context: XmlTag): Array<XmlElementDescriptor> = ArrayUtil.mergeArrays(
        htmlDelegate.getElementsDescriptors(context),
        DescriptorUtil.getTmlSubelementDescriptors(context, namespaceDescriptor)
    )

    /**
     * Returns the descriptor for a specific child tag within the given context.
     * First delegates to the HTML descriptor, then handles XHTML namespace tags
     * for instrumented components or content tags, and finally attempts to get TML tag descriptors.
     *
     * @param childTag the child tag to get the descriptor for
     * @param contextTag the parent context tag
     * @return the element descriptor for the child tag, or null if not found
     */
    override fun getElementDescriptor(childTag: XmlTag, contextTag: XmlTag?): XmlElementDescriptor? {
        htmlDelegate.getElementDescriptor(childTag, contextTag)?.let { return it }

        if (childTag.namespace == XmlUtil.XHTML_URI) {
            if ((contextTag != null && TapestryUtils.getIdentifyingAttribute(contextTag) != null) || isContentTag(contextTag)) {
                return DescriptorUtil.getHtmlTagDescriptorViaNsDescriptor(childTag)
            }
        }

        return DescriptorUtil.getTmlTagDescriptor(childTag)
    }

    /**
     * Returns all possible attribute descriptors for the given context tag.
     * Merges Tapestry component parameters with HTML attributes.
     *
     * @param context the XML tag context, or null for generic descriptors
     * @return array of attribute descriptors combining Tapestry parameters and HTML attributes
     */
    override fun getAttributesDescriptors(context: XmlTag?): Array<XmlAttributeDescriptor> {
        val tapestryAttrs =
            if (context != null) DescriptorUtil.getAttributeDescriptors(context)
            else DescriptorUtil.getAttributeDescriptors(component, null)

        return ArrayUtil.mergeArrays(tapestryAttrs, htmlDelegate.getAttributesDescriptors(context))
    }

    /**
     * Returns the descriptor for a specific attribute by name within the given context.
     * First delegates to the HTML descriptor, then attempts to get Tapestry-specific attribute descriptors.
     *
     * @param attributeName the name of the attribute
     * @param context the XML tag context, or null for component-level descriptors
     * @return the attribute descriptor, or null if not found
     */
    override fun getAttributeDescriptor(attributeName: String, context: XmlTag?): XmlAttributeDescriptor? {
        htmlDelegate.getAttributeDescriptor(attributeName, context)?.let { return it }

        return if (context != null) DescriptorUtil.getAttributeDescriptor(attributeName, context)
        else DescriptorUtil.getAttributeDescriptor(attributeName, component, mixins)
    }

    /**
     * Returns the descriptor for a specific XML attribute.
     * Only handles attributes without namespace prefix, or attributes in Tapestry template or XHTML namespaces.
     *
     * @param attribute the XML attribute
     * @return the attribute descriptor, or null if the namespace is not supported
     */
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

    /**
     * Returns the namespace descriptor for this tag.
     *
     * @return the namespace descriptor delegated from the HTML descriptor
     */
    override fun getNSDescriptor(): XmlNSDescriptor? = htmlDelegate.nsDescriptor

    /**
     * Returns the top-level elements group for this descriptor.
     *
     * @return always null, as no top group is defined
     */
    override fun getTopGroup(): XmlElementsGroup? = null

    /**
     * Returns the content type of this tag descriptor.
     *
     * @return the content type delegated from the HTML descriptor
     */
    override fun getContentType(): Int = htmlDelegate.contentType

    /**
     * Returns the default value for this descriptor.
     *
     * @return always null, as no default value is defined
     */
    override fun getDefaultValue(): String? = null

    /**
     * Returns the PSI element representing the declaration of this descriptor.
     * For Tapestry components, returns the component's Java class; otherwise delegates to HTML descriptor.
     *
     * @return the PSI class element for the component, or the HTML delegate's declaration
     */
    override fun getDeclaration(): PsiElement? =
        if (component != null) component.elementClass
        else htmlDelegate.declaration

    /**
     * Returns the name of this descriptor within the given context.
     *
     * @param context the PSI element context
     * @return the name delegated from the HTML descriptor
     */
    override fun getName(context: PsiElement?): String = htmlDelegate.getName(context)

    /**
     * Returns the name of this descriptor.
     *
     * @return the name delegated from the HTML descriptor
     */
    override fun getName(): String = htmlDelegate.name

    /**
     * Initializes this descriptor with the given PSI element.
     * This implementation performs no initialization.
     *
     * @param element the PSI element to initialize from
     */
    override fun init(element: PsiElement) {}

    /**
     * Sets the name of this descriptor.
     * This implementation performs no operation as the name is immutable.
     *
     * @param name the new name (ignored)
     */
    override fun setName(name: String) {}

    /**
     * Checks if the given tag is a Tapestry content tag.
     * A content tag is identified by the local name "content" in the Tapestry template namespace.
     *
     * @param tag the XML tag to check
     * @return true if the tag is a Tapestry content tag, false otherwise
     */
    private fun isContentTag(tag: XmlTag?): Boolean =
        tag != null && tag.localName == "content" && TapestryXmlExtension.isTapestryTemplateNamespace(tag.namespace)
}
