package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseBooleanResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseLiteralStringResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseNullResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseNumericResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseRangeIntegersResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseThisResolver
import com.github.rar91279.plugin.tapestry.core.util.chain.ChainBase

/**
 * Resolves property values.
 */
class PropResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (getPrefix(context.value, context.defaultPrefix) != PREFIX) return false

        // is this a special case ?
        if (SPECIAL_CASES.execute(context)) return true

        var currentType: IJavaType? = context.contextClass
        var currentContext: ValueResolverContext? = null

        val tokens = (getCleanValue(context.value) ?: return false).split(".").filter { it.isNotEmpty() }

        for ((index, token) in tokens.withIndex()) {
            if (currentType !is IJavaClassType) {
                // a non-class type before the end of the path: nothing further can be resolved
                if (index < tokens.size - 1) return true

                context.resultType = currentType
                context.resultCodeBind = currentContext?.resultCodeBind
                return true
            }

            currentContext = ValueResolverContext(context.project, currentType, token, PREFIX)

            if (!NORMAL_CASES.execute(currentContext)) return false

            currentType = currentContext.resultType
        }

        context.resultType = currentType
        context.resultCodeBind = currentContext?.resultCodeBind

        return true
    }

    private companion object {
        const val PREFIX = "prop"

        val SPECIAL_CASES = ChainBase(
            arrayOf(
                SpecialCaseBooleanResolver(),
                SpecialCaseNullResolver(),
                SpecialCaseThisResolver(),
                SpecialCaseLiteralStringResolver(),
                SpecialCaseNumericResolver(),
                SpecialCaseRangeIntegersResolver()
            )
        )

        val NORMAL_CASES = ChainBase(arrayOf(SingleMethodResolver(), SinglePropertyResolver()))
    }
}
