package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

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
