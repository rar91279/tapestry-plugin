package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter
import com.intellij.xml.XmlAttributeDescriptor

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
        parameter.parameterField?.let { return it }

        val psiClass = component?.elementClass ?: return null
        return psiClass.findFieldByName(parameter.name, true)
    }
}
