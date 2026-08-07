package com.github.rar91279.plugin.tapestry.core.java

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
