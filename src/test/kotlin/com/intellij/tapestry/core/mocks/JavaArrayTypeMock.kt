package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaArrayType
import com.intellij.tapestry.core.java.IJavaType

/**
 * Utility class for easy creation of IJavaArrayType mocks.
 */
class JavaArrayTypeMock(private val _name: String) : IJavaArrayType {

    override fun getName(): String = _name

    override fun isAssignableFrom(type: IJavaType?): Boolean = false

    override fun getUnderlyingObject(): Any = _name

    override fun getComponentType(): IJavaType? = null
}
