package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext

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
