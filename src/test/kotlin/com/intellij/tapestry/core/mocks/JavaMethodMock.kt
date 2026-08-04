package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.java.IJavaAnnotation
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.java.IJavaMethod
import com.intellij.tapestry.core.java.IJavaType
import com.intellij.tapestry.core.java.IMethodParameter

/**
 * Utility class for easy creation of IJavaMethod mocks.
 */
class JavaMethodMock(override val name: String) : IJavaMethod {

    override var returnType: IJavaType? = null
    override var parameters: MutableCollection<IMethodParameter> = ArrayList()
    override val annotations = ArrayList<IJavaAnnotation>()
    override var containingClass: IJavaClassType? = null
    override var documentation: String? = null

    constructor(name: String, returnType: IJavaType?) : this(name) {
        this.returnType = returnType
    }

    constructor(name: String, returnType: IJavaType?, parameters: MutableCollection<IMethodParameter>) : this(name) {
        this.returnType = returnType
        this.parameters = parameters
    }

    fun addParameter(parameter: IMethodParameter): JavaMethodMock {
        parameters.add(parameter)
        return this
    }

    fun addAnnotation(annotation: IJavaAnnotation) {
        annotations.add(annotation)
    }

    override fun getAnnotation(annotationQualifiedName: String?): IJavaAnnotation? =
        annotations.find { it.fullyQualifiedName == annotationQualifiedName }

}
