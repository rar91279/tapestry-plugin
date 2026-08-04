package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.intellij.psi.CommonClassNames
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext

/** Resolves the special case when a property value is given as a boolean literal. */
class SpecialCaseBooleanResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (context.cleanValueLowercased() !in setOf("true", "false")) return false

        context.resultType = context.findType("java.lang.Boolean")
        return true
    }
}

/** Resolves the special case when a property value is given as a null literal. */
class SpecialCaseNullResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (context.cleanValueLowercased() != "null") return false

        context.resultType = context.findType(CommonClassNames.JAVA_LANG_OBJECT)
        return true
    }
}

/** Resolves the special case when a property value is given as a this literal. */
class SpecialCaseThisResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (context.cleanValueLowercased() != "this") return false

        context.resultType = context.contextClass
        return true
    }
}

/** Resolves the special case when a property value is given as a string literal. */
class SpecialCaseLiteralStringResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (!PATTERN.matches(context.cleanValueLowercased() ?: return false)) return false

        context.resultType = context.findType("java.lang.String")
        return true
    }

    private companion object {
        val PATTERN = Regex("'.*'")
    }
}

/** Resolves the special case when a property value is given as a numeric literal. */
class SpecialCaseNumericResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        val cleanValue = context.cleanValueLowercased() ?: return false

        val type = when {
            LONG_PATTERN.matches(cleanValue) -> "java.lang.Long"
            DOUBLE_PATTERN.matches(cleanValue) -> "java.lang.Double"
            else -> return false
        }

        context.resultType = context.findType(type)
        return true
    }

    private companion object {
        val LONG_PATTERN = Regex("\\d+")
        val DOUBLE_PATTERN = Regex("\\d+([.,]\\d+)")
    }
}

/** Resolves the special case when a property value is given as a range of integers. */
class SpecialCaseRangeIntegersResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (!PATTERN.matches(context.cleanValueLowercased() ?: return false)) return false

        context.resultType = context.findType("java.lang.Iterable")
        return true
    }

    private companion object {
        val PATTERN = Regex("\\d+\\.\\.\\d+")
    }
}
