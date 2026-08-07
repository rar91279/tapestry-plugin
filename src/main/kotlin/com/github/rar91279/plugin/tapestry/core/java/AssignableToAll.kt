package com.github.rar91279.plugin.tapestry.core.java

/**
 * A type that is assignable to every other type.
 */
object AssignableToAll : IJavaType {

    @JvmStatic
    fun getInstance(): AssignableToAll = this

    override val name: String = "assignable"

    override fun isAssignableFrom(type: IJavaType?): Boolean = true

    override val underlyingObject: Any get() = this
}
