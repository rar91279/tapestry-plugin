package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
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
