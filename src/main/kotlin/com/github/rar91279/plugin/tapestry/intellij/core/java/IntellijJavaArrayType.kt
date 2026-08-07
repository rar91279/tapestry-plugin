package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiArrayType
import com.github.rar91279.plugin.tapestry.core.java.IJavaArrayType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils

/** [IJavaArrayType] backed by a PSI array type. */
class IntellijJavaArrayType(private val module: Module, private val psiArrayType: PsiArrayType) :
    IntellijJavaType(), IJavaArrayType {

    override val name: String
        get() = psiArrayType.presentableText

    override val underlyingObject: Any
        get() = psiArrayType

    override val componentType: IJavaType?
        get() = IdeaUtils.createJavaTypeFromPsiType(module, psiArrayType.componentType)
}
