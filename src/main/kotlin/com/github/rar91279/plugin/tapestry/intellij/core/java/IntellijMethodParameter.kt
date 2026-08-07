package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.java.IMethodParameter

/** [IMethodParameter] backed by a PSI parameter. */
class IntellijMethodParameter(private val module: Module, private val psiParameter: PsiParameter) : IMethodParameter {

    override val name: String
        get() = psiParameter.name

    override val type: IJavaType?
        get() {
            val psiType = psiParameter.type

            if (psiType is PsiClassType) {
                val containingFile = psiType.resolve()?.containingFile ?: return null
                return IntellijJavaClassType(module, containingFile)
            }

            if (psiType is PsiPrimitiveType) return IntellijJavaPrimitiveType(psiType)

            return null
        }
}
