package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode

/** The argument list of a method call. */
class TelArgumentList(node: ASTNode) : TelCompositeElement(node) {

    val arguments: Array<TelExpression>
        get() = findChildrenByClass(TelExpression::class.java)
}
