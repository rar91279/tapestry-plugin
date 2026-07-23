package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaType
import com.intellij.tapestry.core.java.IMethodParameter

/**
 * Utility class for easy creation of IMethodParameter mocks.
 */
class MethodParameterMock(private val _name: String, private val _type: IJavaType) : IMethodParameter {

    override fun getName(): String = _name

    override fun getType(): IJavaType = _type
}
