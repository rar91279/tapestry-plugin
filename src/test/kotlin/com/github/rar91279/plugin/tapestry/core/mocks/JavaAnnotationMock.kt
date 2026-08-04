package com.github.rar91279.plugin.tapestry.core.mocks

import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation

/**
 * Utility class for easy creation of IJavaAnnotation mocks.
 */
class JavaAnnotationMock() : IJavaAnnotation {

    override var fullyQualifiedName: String? = null
    override val parameters = HashMap<String?, Array<String>>()

    constructor(fullyQualifiedName: String?) : this() {
        this.fullyQualifiedName = fullyQualifiedName
    }

    fun addParameter(name: String, values: Array<String>): JavaAnnotationMock {
        parameters[name] = values
        return this
    }

    fun addParameter(name: String, value: String): JavaAnnotationMock {
        parameters[name] = arrayOf(value)
        return this
    }
}
