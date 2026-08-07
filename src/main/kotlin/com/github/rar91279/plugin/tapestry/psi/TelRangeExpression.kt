package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiType

/** A `from..to` range. */
class TelRangeExpression(node: ASTNode) : TelCompositeElement(node), TelExpression {

    override fun getPsiType(): PsiType? = JavaPsiFacade.getInstance(project).elementFactory
        .createTypeByFQClassName(CommonClassNames.JAVA_UTIL_LIST, resolveScope)
}
