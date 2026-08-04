package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaType
import com.intellij.tapestry.core.java.IMethodParameter

/**
 * Utility class for easy creation of IMethodParameter mocks.
 */
class MethodParameterMock(override val name: String, override val type: IJavaType) : IMethodParameter
