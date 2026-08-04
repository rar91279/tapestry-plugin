package com.github.rar91279.plugin.tapestry.core.java.coercion

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.CommonClassNames
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.AssignableToAll
import com.github.rar91279.plugin.tapestry.core.java.IJavaArrayType
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaPrimitiveType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType

/**
 * Tries to validate if a type coercion is a valid one.
 */
object TypeCoercionValidator {

    private val logger = Logger.getInstance(TypeCoercionValidator::class.java)

    /** A validator returns `null` if the case doesn't apply to it, otherwise its verdict. */
    private val validators = listOf(
        ::validateClassType,
        ::validateArrayType,
        ::validatePrimitiveType,
        ::validateEnumType
    )

    @JvmStatic
    fun canCoerce(
        project: TapestryProject,
        sourceType: IJavaType,
        sourceValue: String?,
        targetType: IJavaType?
    ): Boolean {
        if (targetType == null) return false

        if (sourceType is AssignableToAll || targetType.isAssignableFrom(sourceType)) return true

        val context = CoercionContext(project, sourceType, sourceValue, targetType)

        return try {
            validators.firstNotNullOfOrNull { it(context) } ?: false
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (ex: Exception) {
            logger.error(ex)
            false
        }
    }

    private class CoercionContext(
        val project: TapestryProject,
        val sourceType: IJavaType,
        val sourceValue: String?,
        val targetType: IJavaType
    )

    /**
     * List of all possible coercions between class types.
     * The key is the type to coerce to, the values are the types that can be coerced to the key type.
     */
    private val CLASS_COERCION_MAP = mapOf(
        CommonClassNames.JAVA_LANG_STRING to listOf(CommonClassNames.JAVA_LANG_OBJECT),
        "java.lang.Double" to listOf(
            CommonClassNames.JAVA_LANG_STRING,
            "java.math.BigDecimal",
            "java.lang.Long",
            "java.lang.Float"
        ),
        "java.math.BigDecimal" to listOf(CommonClassNames.JAVA_LANG_STRING),
        "java.math.BigInteger" to listOf(CommonClassNames.JAVA_LANG_STRING),
        "java.lang.Long" to listOf(
            CommonClassNames.JAVA_LANG_STRING,
            "java.lang.Number",
            "org.apache.tapestry5.ioc.util.TimeInterval"
        ),
        "java.lang.Byte" to listOf("java.lang.Long"),
        "java.lang.Short" to listOf("java.lang.Long"),
        "java.lang.Integer" to listOf("java.lang.Long"),
        "java.lang.Float" to listOf("java.lang.Double"),
        "java.lang.Boolean" to listOf(CommonClassNames.JAVA_LANG_OBJECT),
        CommonClassNames.JAVA_UTIL_LIST to listOf(CommonClassNames.JAVA_LANG_OBJECT),
        "org.apache.tapestry5.grid.GridDataSource" to listOf(CommonClassNames.JAVA_UTIL_LIST),
        "org.apache.tapestry5.ioc.util.TimeInterval" to listOf(CommonClassNames.JAVA_LANG_STRING),
        "java.text.DateFormat" to listOf(CommonClassNames.JAVA_LANG_STRING)
    )

    private val PRIMITIVE_COERCION_MAP = mapOf(
        "byte" to "java.lang.Byte",
        "short" to "java.lang.Short",
        "int" to "java.lang.Integer",
        "long" to "java.lang.Long",
        "float" to "java.lang.Float",
        "double" to "java.lang.Double",
        "char" to "java.lang.Character",
        "boolean" to "java.lang.Boolean"
    )

    private fun validateClassType(context: CoercionContext): Boolean? {
        if (context.sourceType !is IJavaClassType || context.targetType !is IJavaClassType) return null

        val coercions = CLASS_COERCION_MAP[context.targetType.fullyQualifiedName] ?: return null

        return coercions.any { typeName ->
            context.project.javaTypeFinder.findType(typeName, true)?.isAssignableFrom(context.sourceType) == true
        }
    }

    private fun validateArrayType(context: CoercionContext): Boolean? {
        val typeFinder = context.project.javaTypeFinder

        if (context.sourceType !is IJavaArrayType) {
            val componentType = (context.targetType as? IJavaArrayType)?.componentType ?: return null
            val objectType = typeFinder.findType(CommonClassNames.JAVA_LANG_OBJECT, true)
            return if (componentType.isAssignableFrom(objectType)) true else null
        }

        // coerce arrays to lists, booleans and grid data sources
        return context.targetType.isAssignableFrom(typeFinder.findType(CommonClassNames.JAVA_UTIL_LIST, true)) ||
               context.targetType.isAssignableFrom(typeFinder.findType("java.lang.Boolean", true)) ||
               (context.targetType as? IJavaClassType)?.fullyQualifiedName == "org.apache.tapestry5.grid.GridDataSource"
    }

    private fun validatePrimitiveType(context: CoercionContext): Boolean? {
        if (context.sourceType !is IJavaPrimitiveType && context.targetType !is IJavaPrimitiveType) return null

        return canCoerce(
            context.project,
            context.sourceType.boxed(context.project) ?: return false,
            context.sourceValue,
            context.targetType.boxed(context.project)
        )
    }

    private fun IJavaType.boxed(project: TapestryProject): IJavaType? {
        if (this !is IJavaPrimitiveType) return this
        val boxedName = PRIMITIVE_COERCION_MAP[name] ?: return this
        return project.javaTypeFinder.findType(boxedName, true)
    }

    private fun validateEnumType(context: CoercionContext): Boolean? {
        val targetType = context.targetType
        if (targetType !is IJavaClassType || !targetType.isEnum) return null

        // this validator is only to coerce strings to enums
        val sourceValue = context.sourceValue ?: return null
        if ((context.sourceType as? IJavaClassType)?.fullyQualifiedName != CommonClassNames.JAVA_LANG_STRING) return null

        return targetType.getFields(true).keys.any { StringUtil.toLowerCase(it) == StringUtil.toLowerCase(sourceValue) }
    }
}
