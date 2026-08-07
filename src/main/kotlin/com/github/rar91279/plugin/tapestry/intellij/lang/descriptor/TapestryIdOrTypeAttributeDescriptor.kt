package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag

/** Descriptor of the `t:id` / `t:type` attributes. */
class TapestryIdOrTypeAttributeDescriptor(private val attributeName: String, private val context: XmlTag) :
    BasicTapestryAttributeDescriptor() {

    override fun getDeclaration(): PsiElement = context

    override fun getName(): String = attributeName
}
