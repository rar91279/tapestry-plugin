package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaField
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
        (parameter.parameterField as? IntellijJavaField)?.let { return it.psiField }

        val psiClass = (component?.elementClass as? IntellijJavaClassType)?.psiClass ?: return null
        return psiClass.findFieldByName(parameter.name, true)
    }
}
