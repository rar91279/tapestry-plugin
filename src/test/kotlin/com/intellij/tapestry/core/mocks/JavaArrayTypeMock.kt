package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaArrayType
import com.intellij.tapestry.core.java.IJavaType

/**
 * Utility class for easy creation of IJavaArrayType mocks.
 */
class JavaArrayTypeMock(override val name: String) : IJavaArrayType {

    override fun isAssignableFrom(type: IJavaType?): Boolean = false

    override val underlyingObject: Any get() = name

    override val componentType: IJavaType? = null
}
