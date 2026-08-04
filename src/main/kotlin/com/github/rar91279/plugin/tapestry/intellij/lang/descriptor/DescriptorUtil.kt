package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.ParameterReceiverElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.TapestryUtils
import com.github.rar91279.plugin.tapestry.psi.TmlFile
import com.intellij.util.ArrayUtil
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlElementDescriptor
import com.intellij.xml.XmlNSDescriptor
import com.intellij.xml.XmlNSDescriptorEx
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor
import com.intellij.xml.impl.schema.XmlNSDescriptorImpl
import com.intellij.xml.util.XmlUtil

/**
 * Builds the tag and attribute descriptors of a Tapestry template.
 */
internal object DescriptorUtil {

    fun getAttributeDescriptors(context: XmlTag): Array<XmlAttributeDescriptor> {
        val result = attributeDescriptorsOfTag(context)
        val mixins = findMixins(context)
        if (mixins.isEmpty()) return result

        val listResult = result.toMutableList()
        for (mixin in mixins) {
            listResult.addAll(getAttributeDescriptors(mixin, null))
        }

        return listResult.toTypedArray()
    }

    fun getAttributeDescriptors(
        component: ParameterReceiverElement?,
        idAttrDescriptor: TapestryIdOrTypeAttributeDescriptor?
    ): Array<XmlAttributeDescriptor> {
        if (component == null) return XmlAttributeDescriptor.EMPTY

        val declaration = idAttrDescriptor?.declaration as? XmlTag
        val additionalParameters =
            declaration?.let { getImplicitHtmlContainer(component, it)?.getAttributesDescriptors(it) }
                ?: XmlAttributeDescriptor.EMPTY

        val descriptors = ArrayList<XmlAttributeDescriptor>()
        component.parameters.values.forEach { descriptors.add(TapestryAttributeDescriptor(it)) }
        if (idAttrDescriptor != null) descriptors.add(idAttrDescriptor)
        descriptors.addAll(additionalParameters)

        return descriptors.toTypedArray()
    }

    fun getAttributeDescriptor(attributeName: String, context: XmlTag): XmlAttributeDescriptor? {
        val prefix = XmlUtil.findPrefixByQualifiedName(attributeName)
        if (prefix.isNotEmpty() && context.getNamespaceByPrefix(prefix).isEmpty()) {
            return null // skip attrs for non defined namespaces
        }

        val attr = TapestryUtils.getIdentifyingAttribute(context)
        if (attr != null && attr.name == attributeName) return TapestryIdOrTypeAttributeDescriptor(attributeName, context)

        val id = getTAttributeName(context, "id")
        if (attributeName == id) return TapestryIdOrTypeAttributeDescriptor(id!!, context)

        val component = TapestryUtils.getTypeOfTag(context)
        getAttributeDescriptor(attributeName, component, findMixins(context))?.let { return it }

        if (component != null) {
            val container = getImplicitHtmlContainer(component, context)
            if (container != null) {
                return container.getAttributeDescriptor(attributeName, context)
                       // allow any unqualified attribute
                       ?: if (!attributeName.contains(':')) AnyXmlAttributeDescriptor(attributeName) else null
            }
        }

        return null
    }

    fun getAttributeDescriptor(
        attributeName: String,
        component: ParameterReceiverElement?,
        mixins: List<Mixin>
    ): XmlAttributeDescriptor? {
        getAttributeDescriptor(attributeName, component)?.let { return it }

        return mixins.firstNotNullOfOrNull { getAttributeDescriptor(attributeName, it) }
    }

    fun getAttributeDescriptor(attributeName: String, component: ParameterReceiverElement?): XmlAttributeDescriptor? {
        val param = component?.parameters?.get(XmlUtil.findLocalNameByQualifiedName(attributeName)) ?: return null

        return TapestryAttributeDescriptor(param)
    }

    fun getTmlSubelementDescriptors(context: XmlTag, descriptor: TapestryNamespaceDescriptor?): Array<XmlElementDescriptor> {
        val project = TapestryModuleSupportLoader.getTapestryProject(context) ?: return XmlElementDescriptor.EMPTY_ARRAY

        val namespacePrefix = context.getPrefixByNamespace(TapestryXmlExtension.getTapestryNamespace(context))
        val namespaceElements = getElementDescriptors(project.availableElements, namespacePrefix, descriptor, context)

        val parametersPrefix = context.getPrefixByNamespace(TapestryConstants.PARAMETERS_NAMESPACE)
        val component = TapestryUtils.getTypeOfTag(context)
        if (parametersPrefix == null || component == null) return namespaceElements

        val parameterElements = getParameterDescriptors(component, parametersPrefix, findMixins(context), descriptor)

        return ArrayUtil.mergeArrays(namespaceElements, parameterElements)
    }

    fun getTmlOrHtmlTagDescriptor(tag: XmlTag): XmlElementDescriptor? {
        val file = getTmlFile(tag) ?: return null

        getTmlTagDescriptor(tag)?.let { return it }

        val htmlDescriptor = getHtmlTagDescriptor(tag, file) ?: return null

        return TapestryHtmlTagDescriptor(
            htmlDescriptor, TapestryUtils.getTypeOfTag(tag), findMixins(tag),
            TapestryXmlExtension.getTapestryTemplateDescriptor(tag)
        )
    }

    fun getHtmlTagDescriptorViaNsDescriptor(tag: XmlTag): XmlElementDescriptor? =
        getTmlFile(tag)?.let { getHtmlTagDescriptor(tag, it) }

    fun getHtmlNSDescriptor(tmlFile: TmlFile): XmlNSDescriptor? =
        tmlFile.document?.getDefaultNSDescriptor(XmlUtil.XHTML_URI, false)

    fun getTmlTagDescriptor(tag: XmlTag): XmlElementDescriptor? {
        val prefix = tag.namespacePrefix
        val tagNamespace = tag.namespace

        if (TapestryXmlExtension.isTapestryTemplateNamespace(tagNamespace)) {
            val component = TapestryUtils.getTypeOfTag(tag)
            val mixins = findMixins(tag)
            val tapestryNamespaceDescriptor = TapestryXmlExtension.getTapestryTemplateDescriptor(tag)

            if (mixins.isEmpty() && component == null) {
                val descriptorFromTapestrySchema = tapestryNamespaceDescriptor?.getElementDescriptor(tag.localName, tagNamespace)
                if (descriptorFromTapestrySchema != null) {
                    return TapestryHtmlTagDescriptor(descriptorFromTapestrySchema, null, mixins, tapestryNamespaceDescriptor)
                }
            }

            return if (component == null) TapestryUnknownTagDescriptor(tag.localName, prefix, tapestryNamespaceDescriptor)
            else TapestryTagDescriptor(component, mixins, prefix, tapestryNamespaceDescriptor)
        }

        if (tagNamespace == TapestryConstants.PARAMETERS_NAMESPACE) {
            val component = tag.parentTag?.let { TapestryUtils.getTypeOfTag(it) }
            val parameterName = tag.localName
            val parameter = component?.parameters?.get(parameterName)
            val tapestryNamespaceDescriptor = TapestryXmlExtension.getTapestryTemplateDescriptor(tag)

            return if (parameter == null) TapestryUnknownTagDescriptor(parameterName, prefix, tapestryNamespaceDescriptor)
            else TapestryParameterDescriptor(component, parameter, prefix, tapestryNamespaceDescriptor)
        }

        return null
    }

    fun getTAttributeName(context: XmlTag, attrName: String): String? {
        val prefix = context.getPrefixByNamespace(TapestryXmlExtension.getTapestryNamespace(context)) ?: return null

        return if (prefix.isNotEmpty()) "$prefix:$attrName" else attrName
    }

    private fun attributeDescriptorsOfTag(context: XmlTag): Array<XmlAttributeDescriptor> {
        val component = TapestryUtils.getTypeOfTag(context)
        val id = getTAttributeName(context, "id")

        if (component != null) {
            return getAttributeDescriptors(component, id?.let { TapestryIdOrTypeAttributeDescriptor(it, context) })
        }
        if (id == null) return XmlAttributeDescriptor.EMPTY

        val type = getTAttributeName(context, "type")

        return arrayOf(
            TapestryIdOrTypeAttributeDescriptor(type.orEmpty(), context),
            TapestryIdOrTypeAttributeDescriptor(id, context)
        )
    }

    private fun getImplicitHtmlContainer(component: ParameterReceiverElement, context: XmlTag): XmlElementDescriptor? {
        if (!component.elementClass.supportsInformalParameters()) return null

        val descriptor = context.getNSDescriptor(XmlUtil.XHTML_URI, false)

        return (descriptor as? XmlNSDescriptorEx)?.getElementDescriptor("div", XmlUtil.XHTML_URI)
    }

    private fun getElementDescriptors(
        elements: Collection<PresentationLibraryElement>,
        namespacePrefix: String?,
        descriptor: TapestryNamespaceDescriptor?,
        context: XmlTag
    ): Array<XmlElementDescriptor> {
        val descriptors: Array<XmlElementDescriptor> =
            elements.map { TapestryTagDescriptor(it, namespacePrefix, descriptor) }.toTypedArray()

        val descriptorsFromSchema = descriptor
            ?.getSuperRootElementsDescriptors(PsiTreeUtil.getParentOfType(context, XmlDocument::class.java))
            ?: XmlElementDescriptor.EMPTY_ARRAY

        return ArrayUtil.mergeArrays(descriptors, descriptorsFromSchema)
    }

    private fun getParameterDescriptors(
        component: TapestryComponent,
        namespacePrefix: String?,
        mixins: List<Mixin>,
        descriptor: TapestryNamespaceDescriptor?
    ): Array<XmlElementDescriptor> {
        val result = ArrayList<XmlElementDescriptor>()

        component.parameters.values.forEach { result.add(TapestryParameterDescriptor(component, it, namespacePrefix, descriptor)) }
        for (mixin in mixins) {
            mixin.parameters.values.forEach { result.add(TapestryParameterDescriptor(component, it, namespacePrefix, descriptor)) }
        }

        return result.toTypedArray()
    }

    private fun getHtmlTagDescriptor(tag: XmlTag, file: TmlFile): XmlElementDescriptor? {
        val htmlNSDescriptor = getHtmlNSDescriptor(file) ?: return null

        return if (htmlNSDescriptor is XmlNSDescriptorImpl) htmlNSDescriptor.getElementDescriptor(tag.localName, tag.namespace)
        else htmlNSDescriptor.getElementDescriptor(tag)
    }

    private fun getTmlFile(tag: XmlTag): TmlFile? {
        val file: PsiFile? = tag.containingFile
        if (file is TmlFile) return file

        val parentTag = tag.getUserData(XmlElement.INCLUDING_ELEMENT) ?: return null

        return parentTag.containingFile as? TmlFile
    }

    private fun findMixins(tag: XmlTag?): List<Mixin> {
        if (tag == null) return emptyList()

        val tapestryProject = TapestryUtils.getTapestryProject(tag) ?: return emptyList()
        val mixinsAttribute = tag.getAttribute("mixins", TapestryXmlExtension.getTapestryNamespace(tag)) ?: return emptyList()

        return mixinsAttribute.value.orEmpty()
            .split(",")
            .filter { it.isNotEmpty() }
            .mapNotNull { tapestryProject.findMixin(it) }
    }
}
