package com.github.rar91279.plugin.tapestry.core.java

import com.github.rar91279.plugin.tapestry.core.resource.IResource

/**
 * Represents a JAVA class type.
 */
interface IJavaClassType : IJavaType {

    val fullyQualifiedName: String?

    val isInterface: Boolean

    val isPublic: Boolean

    fun hasDefaultConstructor(): Boolean

    val superClassType: IJavaClassType?

    val isEnum: Boolean

    /**
     * @param fromSuper indicates if methods from super classes should also be returned.
     * @return the public methods declared in the type.
     */
    fun getPublicMethods(fromSuper: Boolean): Collection<IJavaMethod>

    /**
     * @param fromSuper indicates if methods from super classes should also be returned.
     * @return all the methods declared in the type.
     */
    fun getAllMethods(fromSuper: Boolean): Collection<IJavaMethod>

    /**
     * @return all public methods whose name matches the given regexp.
     */
    fun findPublicMethods(methodNameRegExp: String): Collection<IJavaMethod>

    val annotations: Collection<IJavaAnnotation>

    /**
     * @param fromSuper indicates if fields from super classes should also be returned.
     * @return the fields declared in the type.
     */
    fun getFields(fromSuper: Boolean): Map<String, IJavaField>

    /** The javadoc description of the type. */
    val documentation: String?

    /** The file that contains this class. */
    val file: IResource?

    fun supportsInformalParameters(): Boolean
}
