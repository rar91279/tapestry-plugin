package com.github.rar91279.plugin.tapestry.core.model.externalizable

import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType

/**
 * Every class that implements this has a representation that can be included in a class.
 */
interface ExternalizableToClass {

    @Throws(Exception::class)
    fun getClassRepresentation(targetClass: IJavaClassType): String?
}
