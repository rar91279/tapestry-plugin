package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext

/** Resolves the special case when a property value is given as a this literal. */
class SpecialCaseThisResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (context.cleanValueLowercased() != "this") return false

        context.resultType = context.contextClass
        return true
    }
}
