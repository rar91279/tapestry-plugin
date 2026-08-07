package com.github.rar91279.plugin.tapestry.core.java

/**
 * Represents a JAVA method.
 */
interface IJavaMethod {

    val name: String?

    val returnType: IJavaType?

    val parameters: Collection<IMethodParameter>

    val annotations: Collection<IJavaAnnotation>

    /**
     * @return the annotation of the method with the given qualified name.
     */
    fun getAnnotation(annotationQualifiedName: String?): IJavaAnnotation?

    /** The class that contains this method. */
    val containingClass: IJavaClassType?

    /** The javadoc description of the method. */
    val documentation: String?
}
