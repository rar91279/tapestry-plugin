package com.github.rar91279.plugin.tapestry.intellij.lang.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttributeValue

/** A reference from an attribute value to an already known element. */
class PsiAttributeValueReference(attributeValue: XmlAttributeValue, private val bindElement: PsiElement?) :
    PsiReferenceBase<XmlAttributeValue>(attributeValue) {

    override fun resolve(): PsiElement? = bindElement
}
