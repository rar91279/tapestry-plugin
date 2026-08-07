package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext

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
