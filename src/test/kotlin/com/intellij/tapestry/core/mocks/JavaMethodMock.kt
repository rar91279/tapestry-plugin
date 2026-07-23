package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaAnnotation
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.java.IJavaMethod
import com.intellij.tapestry.core.java.IJavaType
import com.intellij.tapestry.core.java.IMethodParameter

/**
 * Utility class for easy creation of IJavaMethod mocks.
 */
class JavaMethodMock(private val _name: String) : IJavaMethod {

    private var _returnType: IJavaType? = null
    private var _parameters: MutableCollection<IMethodParameter> = ArrayList()
    private val _annotations = ArrayList<IJavaAnnotation>()
    private var _containingClass: IJavaClassType? = null
    private var _documentation: String? = null

    constructor(name: String, returnType: IJavaType?) : this(name) {
        _returnType = returnType
    }

    constructor(name: String, returnType: IJavaType?, parameters: MutableCollection<IMethodParameter>) : this(name) {
        _returnType = returnType
        _parameters = parameters
    }

    override fun getName(): String = _name

    override fun getReturnType(): IJavaType? = _returnType

    override fun getParameters(): Collection<IMethodParameter> = _parameters

    fun addParameter(parameter: IMethodParameter): JavaMethodMock {
        _parameters.add(parameter)
        return this
    }

    override fun getAnnotations(): Collection<IJavaAnnotation> = _annotations

    fun addAnnotation(annotation: IJavaAnnotation) {
        _annotations.add(annotation)
    }

    override fun getAnnotation(annotationQualifiedName: String): IJavaAnnotation? {
        for (annotation in _annotations) {
            if (annotation.fullyQualifiedName == annotationQualifiedName) {
                return annotation
            }
        }
        return null
    }

    override fun getContainingClass(): IJavaClassType? = _containingClass

    fun setContainingClass(containingClass: IJavaClassType?) {
        _containingClass = containingClass
    }

    override fun getDocumentation(): String? = _documentation

    fun setDocumentation(documentation: String?) {
        _documentation = documentation
    }
}
