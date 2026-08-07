package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType

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
