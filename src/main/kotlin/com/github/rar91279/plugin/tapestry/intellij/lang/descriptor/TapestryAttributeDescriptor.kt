package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter

/** Descriptor of a component parameter attribute. */
class TapestryAttributeDescriptor(private val param: TapestryParameter) : BasicTapestryAttributeDescriptor() {

    override fun getDeclaration(): PsiElement? = param.parameterField // else: built in attribute

    override fun getName(): String = param.name

    override fun isRequired(): Boolean = param.isRequired

    override fun getDefaultValue(): String? = param.defaultValue

    val defaultPrefix: String?
        get() = param.defaultPrefix
}
