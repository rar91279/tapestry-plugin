package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.psi.PsiPrimitiveType
import com.github.rar91279.plugin.tapestry.core.java.IJavaPrimitiveType

/** [IJavaPrimitiveType] backed by a PSI primitive type. */
class IntellijJavaPrimitiveType(private val psiPrimitiveType: PsiPrimitiveType) : IntellijJavaType(), IJavaPrimitiveType {

    override val name: String
        get() = psiPrimitiveType.presentableText

    override val underlyingObject: Any
        get() = psiPrimitiveType
}
