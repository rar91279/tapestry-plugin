package com.github.rar91279.plugin.tapestry.core.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.github.rar91279.plugin.tapestry.core.TapestryProject

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

    fun canCoerce(
        project: TapestryProject,
        sourceType: PsiType,
        sourceValue: String?,
        targetType: PsiType?
    ): Boolean {
        if (targetType == null) return false

        // The null type stands in for "accepts anything", produced by the component and validate
        // resolvers. Kept as an explicit check rather than relying on null-type assignability, which
        // differs for primitive targets.
        if (sourceType == PsiTypes.nullType() || targetType.isAssignableFrom(sourceType)) return true

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
        val sourceType: PsiType,
        val sourceValue: String?,
        val targetType: PsiType
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
        if (context.sourceType !is PsiClassType || context.targetType !is PsiClassType) return null

        val coercions = CLASS_COERCION_MAP[context.targetType.resolve()?.qualifiedName] ?: return null

        return coercions.any { typeName ->
            context.project.findClassType(typeName)?.isAssignableFrom(context.sourceType) == true
        }
    }

    private fun validateArrayType(context: CoercionContext): Boolean? {
        val project = context.project

        if (context.sourceType !is PsiArrayType) {
            val componentType = (context.targetType as? PsiArrayType)?.componentType ?: return null
            val objectType = project.findClassType(CommonClassNames.JAVA_LANG_OBJECT) ?: return null
            return if (componentType.isAssignableFrom(objectType)) true else null
        }

        // coerce arrays to lists, booleans and grid data sources
        return project.findClassType(CommonClassNames.JAVA_UTIL_LIST)?.let { context.targetType.isAssignableFrom(it) } == true ||
               project.findClassType("java.lang.Boolean")?.let { context.targetType.isAssignableFrom(it) } == true ||
               (context.targetType as? PsiClassType)?.resolve()?.qualifiedName == "org.apache.tapestry5.grid.GridDataSource"
    }

    private fun validatePrimitiveType(context: CoercionContext): Boolean? {
        if (context.sourceType !is PsiPrimitiveType && context.targetType !is PsiPrimitiveType) return null

        return canCoerce(
            context.project,
            context.sourceType.boxed(context.project) ?: return false,
            context.sourceValue,
            context.targetType.boxed(context.project)
        )
    }

    private fun PsiType.boxed(project: TapestryProject): PsiType? {
        if (this !is PsiPrimitiveType) return this
        val boxedName = PRIMITIVE_COERCION_MAP[presentableText] ?: return this
        return project.findClassType(boxedName)
    }

    private fun validateEnumType(context: CoercionContext): Boolean? {
        val targetClass = (context.targetType as? PsiClassType)?.resolve() ?: return null
        if (!targetClass.isEnum) return null

        // this validator is only to coerce strings to enums
        val sourceValue = context.sourceValue ?: return null
        if ((context.sourceType as? PsiClassType)?.resolve()?.qualifiedName != CommonClassNames.JAVA_LANG_STRING) return null

        return targetClass.tapestryFields(true).keys.any { it.lowercase() == sourceValue.lowercase() }
    }
}
