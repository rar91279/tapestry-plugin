package com.github.rar91279.plugin.tapestry.core.java

/**
 * Represents a JAVA type.
 */
interface IJavaType {

    val name: String?

    /**
     * Tests whether a given type can be converted to the type represented by this object.
     */
    fun isAssignableFrom(type: IJavaType?): Boolean

    /** The underlying object of this class. This is usually an IDE specific object. */
    val underlyingObject: Any?
}
