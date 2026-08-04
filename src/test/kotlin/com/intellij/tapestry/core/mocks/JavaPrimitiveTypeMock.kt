package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaPrimitiveType
import com.intellij.tapestry.core.java.IJavaType

/**
 * Utility class for easy creation of JavaPrimitiveType mocks.
 */
class JavaPrimitiveTypeMock(override val name: String) : IJavaPrimitiveType {

    override fun isAssignableFrom(type: IJavaType?): Boolean = false

    override val underlyingObject: Any get() = name
}
