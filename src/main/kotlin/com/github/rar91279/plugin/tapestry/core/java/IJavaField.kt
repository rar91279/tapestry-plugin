package com.github.rar91279.plugin.tapestry.core.java

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
