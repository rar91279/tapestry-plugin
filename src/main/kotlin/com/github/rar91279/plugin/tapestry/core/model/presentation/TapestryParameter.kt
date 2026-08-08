package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.util.attributeValues
import com.github.rar91279.plugin.tapestry.core.util.erasedIfTypeVariable
import com.github.rar91279.plugin.tapestry.core.util.javadocDescription
import com.github.rar91279.plugin.tapestry.core.util.tapestryMethods
import com.intellij.openapi.util.text.StringUtil.capitalize
import com.intellij.openapi.util.text.StringUtil.notNullize
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiType

/**
 * A Tapestry parameter.
 *
 * [parameterField] is `null` for the built-in components' parameters, which have no class and so no
 * declaring field; [DummyTapestryParameter] overrides everything that would read one.
 */
open class TapestryParameter(
    private val elementClass: PsiClass?,
    val parameterField: PsiField?
) : Comparable<TapestryParameter> {

    private val validField: PsiField?
        get() = parameterField?.takeIf { it.isValid }

    private val paramAnnotation: PsiAnnotation?
        get() = parameterField?.getAnnotation(PresentationLibraryElement.PARAMETER_ANNOTATION)

    /** The declared type of the parameter. */
    open val type: PsiType?
        get() = parameterField?.type?.erasedIfTypeVariable()

    /**
     * The parameter name, either defined in the parameter annotation or taken from the field name.
     */
    open val name: String
        get() {
            val field = validField ?: return ""

            val name = paramAnnotation?.attributeValues(PARAMETER_NAME)?.firstOrNull() ?: field.name

            return name.removePrefix("$").removePrefix("_")
        }

    /**
     * The parameter description.
     */
    open val description: String?
        get() = validField?.javadocDescription() ?: ""

    /**
     * `true` if the parameter is required.
     */
    open val isRequired: Boolean
        get() {
            validField ?: return false

            val required = paramAnnotation?.attributeValues("required")?.firstOrNull() == true.toString()

            return required && !hasMethod(elementClass, name)
        }

    /**
     * The default prefix of the parameter value.
     */
    open val defaultPrefix: String
        get() {
            validField ?: return ""

            return paramAnnotation?.attributeValues("defaultPrefix")?.firstOrNull() ?: "prop"
        }

    /**
     * The default value of the parameter.
     */
    open val defaultValue: String
        get() {
            validField ?: return ""

            return paramAnnotation?.attributeValues("value")?.firstOrNull() ?: ""
        }

    override fun compareTo(other: TapestryParameter): Int = name.compareTo(other.name)

    override fun equals(other: Any?): Boolean = other is TapestryParameter && name == other.name

    override fun hashCode(): Int = name.hashCode()

    private companion object {

        const val PARAMETER_NAME = "name"

        fun hasMethod(clazz: PsiClass?, methodName: String): Boolean {
            val defaultMethod = TapestryConstants.DEFAULT_PARAMETER_METHOD_PREFIX + capitalize(notNullize(methodName))

            return clazz?.tapestryMethods(true)?.any {
                it.name == defaultMethod && it.parameterList.isEmpty
            } == true
        }
    }
}
