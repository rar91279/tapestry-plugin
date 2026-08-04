package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaAnnotation
import com.intellij.tapestry.core.java.IJavaField
import com.intellij.tapestry.core.java.IJavaType

/**
 * Utility class for easy creation of IJavaField mocks.
 */
class JavaFieldMock() : IJavaField {

    override var name: String? = null
    override var type: IJavaType? = null
    override var isPrivate = false
    override val annotations = HashMap<String, IJavaAnnotation>()
    override var documentation: String? = null
    override var stringRepresentation: String? = null
    override val isValid = true

    constructor(name: String?, aPrivate: Boolean) : this() {
        this.name = name
        isPrivate = aPrivate
    }

    fun setName(name: String?): JavaFieldMock {
        this.name = name
        return this
    }

    fun setType(type: IJavaType?): JavaFieldMock {
        this.type = type
        return this
    }

    fun setPrivate(aPrivate: Boolean): JavaFieldMock {
        isPrivate = aPrivate
        return this
    }

    fun addAnnotation(annotation: IJavaAnnotation): JavaFieldMock {
        annotations[annotation.fullyQualifiedName!!] = annotation
        return this
    }

    fun setDocumentation(documentation: String?): JavaFieldMock {
        this.documentation = documentation
        return this
    }

    fun setStringRepresentation(stringRepresentation: String?): JavaFieldMock {
        this.stringRepresentation = stringRepresentation
        return this
    }
}
