package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiType

/** A `method(args)` call. */
class TelMethodCallExpression(node: ASTNode) : TelCompositeElement(node), TelReferenceQualifier {

    val argumentTypes: Array<PsiType?>
        get() = argumentList.arguments.map { it.getPsiType() }.toTypedArray()

    val argumentList: TelArgumentList
        get() = findNotNullChildByClass(TelArgumentList::class.java)

    override fun getPsiType(): PsiType? = findNotNullChildByClass(TelReferenceExpression::class.java).getPsiType()
}
