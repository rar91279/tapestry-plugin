package com.github.rar91279.plugin.tapestry.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType

/** An expression of the Tapestry Expression Language. */
interface TelExpression : PsiElement {

    fun getPsiType(): PsiType?
}
