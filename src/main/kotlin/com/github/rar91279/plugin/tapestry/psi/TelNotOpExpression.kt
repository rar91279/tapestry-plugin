package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes

/** A `!expression` negation. */
class TelNotOpExpression(node: ASTNode) : TelCompositeElement(node), TelExpression {

    override fun getPsiType(): PsiType = PsiTypes.booleanType()
}
