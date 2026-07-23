package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaAnnotation
import com.intellij.tapestry.core.java.IJavaField
import com.intellij.tapestry.core.java.IJavaType

/**
 * Utility class for easy creation of IJavaField mocks.
 */
class JavaFieldMock() : IJavaField {

    private var _name: String? = null
    private var _type: IJavaType? = null
    private var _private = false
    private val _annotations = HashMap<String, IJavaAnnotation>()
    private var _documentation: String? = null
    private var _stringRepresentation: String? = null

    constructor(name: String?, aPrivate: Boolean) : this() {
        _name = name
        _private = aPrivate
    }

    override fun getName(): String? = _name

    fun setName(name: String?): JavaFieldMock {
        _name = name
        return this
    }

    override fun getType(): IJavaType? = _type

    fun setType(type: IJavaType?): JavaFieldMock {
        _type = type
        return this
    }

    override fun isPrivate(): Boolean = _private

    fun setPrivate(aPrivate: Boolean): JavaFieldMock {
        _private = aPrivate
        return this
    }

    override fun getAnnotations(): Map<String, IJavaAnnotation> = _annotations

    fun addAnnotation(annotation: IJavaAnnotation): JavaFieldMock {
        _annotations[annotation.fullyQualifiedName] = annotation
        return this
    }

    override fun getDocumentation(): String? = _documentation

    fun setDocumentation(documentation: String?): JavaFieldMock {
        _documentation = documentation
        return this
    }

    override fun getStringRepresentation(): String? = _stringRepresentation

    fun setStringRepresentation(stringRepresentation: String?): JavaFieldMock {
        _stringRepresentation = stringRepresentation
        return this
    }

    override fun isValid(): Boolean = true
}
