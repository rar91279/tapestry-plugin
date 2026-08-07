package com.github.rar91279.plugin.tapestry.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

/** Base class of the composite TEL elements. */
open class TelCompositeElement(node: ASTNode) : ASTWrapperPsiElement(node) {

    override fun toString(): String = node.elementType.toString()
}
