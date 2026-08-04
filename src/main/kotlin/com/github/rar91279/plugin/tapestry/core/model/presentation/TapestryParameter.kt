package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.intellij.openapi.util.text.StringUtil.capitalize
import com.intellij.openapi.util.text.StringUtil.notNullize
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaField

/**
 * A Tapestry parameter.
 */
open class TapestryParameter(
    private val elementClass: IJavaClassType?,
    val parameterField: IJavaField
) : Comparable<TapestryParameter> {

    private val paramAnnotation: IJavaAnnotation?
        get() = parameterField.annotations[PresentationLibraryElement.PARAMETER_ANNOTATION]

    /**
     * The parameter name, either defined in the parameter annotation or taken from the field name.
     */
    open val name: String
        get() {
            if (!parameterField.isValid) return ""

            val name = paramAnnotation?.parameters?.get(PARAMETER_NAME)?.get(0) ?: parameterField.name ?: ""

            return name.removePrefix("$").removePrefix("_")
        }

    /**
     * The parameter description.
     */
    open val description: String?
        get() = if (parameterField.isValid) parameterField.documentation else ""

    /**
     * `true` if the parameter is required.
     */
    open val isRequired: Boolean
        get() {
            if (!parameterField.isValid) return false

            val required = paramAnnotation?.parameters?.get("required")?.get(0) == true.toString()

            return required && !hasMethod(elementClass, name)
        }

    /**
     * The default prefix of the parameter value.
     */
    open val defaultPrefix: String
        get() {
            if (!parameterField.isValid) return ""

            return paramAnnotation?.parameters?.get("defaultPrefix")?.get(0) ?: "prop"
        }

    /**
     * The default value of the parameter.
     */
    open val defaultValue: String
        get() {
            if (!parameterField.isValid) return ""

            return paramAnnotation?.parameters?.get("value")?.get(0) ?: ""
        }

    override fun compareTo(other: TapestryParameter): Int = name.compareTo(other.name)

    override fun equals(other: Any?): Boolean = other is TapestryParameter && name == other.name

    override fun hashCode(): Int = name.hashCode()

    private companion object {

        const val PARAMETER_NAME = "name"

        fun hasMethod(clazz: IJavaClassType?, methodName: String): Boolean {
            val defaultMethod = TapestryConstants.DEFAULT_PARAMETER_METHOD_PREFIX + capitalize(notNullize(methodName))

            return clazz?.getAllMethods(true)?.any { it.name == defaultMethod && it.parameters.isEmpty() } == true
        }
    }
}
