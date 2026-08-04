package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.github.rar91279.plugin.tapestry.core.java.IJavaArrayType
import com.github.rar91279.plugin.tapestry.core.java.IJavaPrimitiveType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.java.IMethodParameter
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils

/** Base class of the PSI backed [IJavaType] implementations. */
abstract class IntellijJavaType : IJavaType {

    override fun isAssignableFrom(type: IJavaType?): Boolean {
        if (type == null) return false

        val thisType = underlyingObject as? PsiType
        if (thisType == null) {
            logger.warn("The type \"$name\" didn't have a valid underlying object so correct usage of the type wasn't possible.")
            return false
        }

        val otherType = type.underlyingObject as? PsiType
        if (otherType == null) {
            logger.warn("The type \"${type.name}\" didn't have a valid underlying object so correct execution of isAssignableFrom wasn't possible.")
            return false
        }

        return thisType.isAssignableFrom(otherType)
    }

    private companion object {
        val logger = Logger.getInstance(IntellijJavaType::class.java)
    }
}

/** [IJavaPrimitiveType] backed by a PSI primitive type. */
class IntellijJavaPrimitiveType(private val psiPrimitiveType: PsiPrimitiveType) : IntellijJavaType(), IJavaPrimitiveType {

    override val name: String
        get() = psiPrimitiveType.presentableText

    override val underlyingObject: Any
        get() = psiPrimitiveType
}

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
