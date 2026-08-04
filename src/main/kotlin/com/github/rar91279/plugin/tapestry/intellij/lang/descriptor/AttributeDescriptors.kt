package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaField
import com.intellij.xml.impl.BasicXmlAttributeDescriptor

/**
 * Base class of the Tapestry attribute descriptors: a plain, non-enumerated, optional attribute.
 */
abstract class BasicTapestryAttributeDescriptor : BasicXmlAttributeDescriptor() {

    override fun init(element: PsiElement) {}

    override fun isRequired(): Boolean = false

    override fun isFixed(): Boolean = false

    override fun hasIdType(): Boolean = false

    override fun hasIdRefType(): Boolean = false

    override fun isEnumerated(): Boolean = false

    override fun getEnumeratedValues(): Array<String>? = null

    override fun getDefaultValue(): String? = null
}

/** Descriptor of a component parameter attribute. */
class TapestryAttributeDescriptor(private val param: TapestryParameter) : BasicTapestryAttributeDescriptor() {

    override fun getDeclaration(): PsiElement? = (param.parameterField as? IntellijJavaField)?.psiField // else: built in attribute

    override fun getName(): String = param.name

    override fun isRequired(): Boolean = param.isRequired

    override fun getDefaultValue(): String? = param.defaultValue

    val defaultPrefix: String?
        get() = param.defaultPrefix
}

/** Descriptor of the `t:id` / `t:type` attributes. */
class TapestryIdOrTypeAttributeDescriptor(private val attributeName: String, private val context: XmlTag) :
    BasicTapestryAttributeDescriptor() {

    override fun getDeclaration(): PsiElement = context

    override fun getName(): String = attributeName
}

/** Descriptor of the `mixin` attribute. */
class TapestryMixinDescriptor : BasicTapestryAttributeDescriptor() {

    override fun getDeclaration(): PsiElement? = null

    override fun getName(): String = "mixin"
}
