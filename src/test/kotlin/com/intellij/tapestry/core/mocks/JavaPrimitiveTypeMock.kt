package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaPrimitiveType
import com.intellij.tapestry.core.java.IJavaType

/**
 * Utility class for easy creation of JavaPrimitiveType mocks.
 */
class JavaPrimitiveTypeMock(private val _name: String) : IJavaPrimitiveType {

    override fun getName(): String = _name

    override fun isAssignableFrom(type: IJavaType?): Boolean = false

    override fun getUnderlyingObject(): Any = _name
}
