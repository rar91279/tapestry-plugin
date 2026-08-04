package com.intellij.tapestry.core.java

import com.intellij.tapestry.core.ioc.IServiceBindingDiscoverer
import com.intellij.tapestry.core.resource.IResource

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

/** A Java primitive type. */
interface IJavaPrimitiveType : IJavaType

/** A Java array type. */
interface IJavaArrayType : IJavaType {

    val componentType: IJavaType?
}

interface IJavaAnnotation {

    val fullyQualifiedName: String?

    /** Annotation attribute values by name; the default `value` attribute is keyed `null`. */
    val parameters: Map<String?, Array<String>>
}

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

/**
 * Represents a JAVA field.
 */
interface IJavaField {

    val name: String?

    val type: IJavaType?

    val isPrivate: Boolean

    val annotations: Map<String, IJavaAnnotation>

    /** The javadoc description of the field. */
    val documentation: String?

    /** The string representation of the declaration of this field. */
    val stringRepresentation: String?

    val isValid: Boolean
}

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

/**
 * Represents a JAVA method parameter.
 */
interface IMethodParameter {

    val name: String?

    val type: IJavaType?
}

/**
 * Creates JAVA element instances.
 */
interface IJavaTypeCreator {

    /**
     * Creates a new field.
     *
     * @param changeNameToReflectIdeSettings `true` if the IDE coding style should be used to change the field name accordingly.
     */
    fun createField(name: String, type: IJavaClassType, isPrivate: Boolean, changeNameToReflectIdeSettings: Boolean): IJavaField?

    /**
     * Creates a new field annotation and adds it to the field.
     */
    fun createFieldAnnotation(field: IJavaField, fullyQualifiedName: String, parameters: Map<String, String>): IJavaAnnotation?

    /**
     * Ensures that a type is in the import list of a class.
     *
     * @return `true` if the import was insured, `false` otherwise.
     */
    fun ensureClassImport(baseClass: IJavaClassType, type: IJavaClassType): Boolean
}

/**
 * Searches for JAVA types in the project.
 */
interface IJavaTypeFinder {

    /**
     * @return the type with the given fully qualified name, `null` if none is found.
     */
    fun findType(fullyQualifiedName: String, includeDependencies: Boolean): IJavaClassType?

    /**
     * @return all the JAVA types declared in the given package.
     */
    fun findTypesInPackage(packageName: String, includeDependencies: Boolean): Collection<IJavaClassType>

    /**
     * @return all the JAVA types declared in the given package and it's sub-packages.
     */
    fun findTypesInPackageRecursively(basePackageName: String, includeDependencies: Boolean): Collection<IJavaClassType>

    val serviceBindingDiscoverer: IServiceBindingDiscoverer?
}

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
