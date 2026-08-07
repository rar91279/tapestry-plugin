package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.intellij.xml.XmlAttributeDescriptor

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
