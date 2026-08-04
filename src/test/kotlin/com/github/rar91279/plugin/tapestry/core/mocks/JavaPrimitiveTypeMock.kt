package com.github.rar91279.plugin.tapestry.core.mocks

import com.github.rar91279.plugin.tapestry.core.java.IJavaPrimitiveType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType

/**
 * Utility class for easy creation of JavaPrimitiveType mocks.
 */
class JavaPrimitiveTypeMock(override val name: String) : IJavaPrimitiveType {

    override fun isAssignableFrom(type: IJavaType?): Boolean = false

    override val underlyingObject: Any get() = name
}
