package com.github.rar91279.plugin.tapestry.core.model.externalizable

import com.intellij.psi.PsiClass

/**
 * Every class that implements this has a representation that can be included in a class.
 */
interface ExternalizableToClass {

    @Throws(Exception::class)
    fun getClassRepresentation(targetClass: PsiClass): String?
}
