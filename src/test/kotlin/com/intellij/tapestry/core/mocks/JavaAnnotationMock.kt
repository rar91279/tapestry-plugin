package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaAnnotation

/**
 * Utility class for easy creation of IJavaAnnotation mocks.
 */
class JavaAnnotationMock() : IJavaAnnotation {

    private var _fullyQualifiedName: String? = null
    private val _parameters = HashMap<String, Array<String>>()

    constructor(fullyQualifiedName: String?) : this() {
        _fullyQualifiedName = fullyQualifiedName
    }

    override fun getFullyQualifiedName(): String? = _fullyQualifiedName

    fun setFullyQualifiedName(fullyQualifiedName: String?) {
        _fullyQualifiedName = fullyQualifiedName
    }

    override fun getParameters(): Map<String, Array<String>> = _parameters

    fun addParameter(name: String, values: Array<String>): JavaAnnotationMock {
        _parameters[name] = values
        return this
    }

    fun addParameter(name: String, value: String): JavaAnnotationMock {
        _parameters[name] = arrayOf(value)
        return this
    }
}
