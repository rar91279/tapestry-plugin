package com.github.rar91279.plugin.tapestry.core.mocks

import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.java.IMethodParameter

/**
 * Utility class for easy creation of IMethodParameter mocks.
 */
class MethodParameterMock(override val name: String, override val type: IJavaType) : IMethodParameter
