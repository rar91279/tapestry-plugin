package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver.Companion.resolveWith
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseBooleanResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseLiteralStringResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseNullResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseNumericResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseRangeIntegersResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseThisResolver

/**
 * Resolves property values.
 */
class PropResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (getPrefix(context.value, context.defaultPrefix) != PREFIX) return false

        // is this a special case ?
        if (SPECIAL_CASES.resolveWith(context)) return true

        // Two variables where the wrapper layer needed one: PSI keeps the declaration (PsiClass, what the
        // next path segment is resolved against) apart from the type (PsiType, what the value resolves to).
        var currentClass: PsiClass? = context.contextClass
        var currentType: PsiType? = currentClass?.let { context.project.classTypeOf(it) }
        var currentContext: ValueResolverContext? = null

        val tokens = (getCleanValue(context.value) ?: return false).split(".").filter { it.isNotEmpty() }

        for ((index, token) in tokens.withIndex()) {
            val resolvedClass = currentClass
            if (resolvedClass == null) {
                // a non-class type before the end of the path: nothing further can be resolved
                if (index < tokens.size - 1) return true

                context.resultType = currentType
                context.resultCodeBind = currentContext?.resultCodeBind
                return true
            }

            currentContext = ValueResolverContext(context.project, resolvedClass, token, PREFIX)

            if (!NORMAL_CASES.resolveWith(currentContext)) return false

            currentType = currentContext.resultType
            currentClass = (currentType as? PsiClassType)?.resolve()
        }

        context.resultType = currentType
        context.resultCodeBind = currentContext?.resultCodeBind

        return true
    }

    private companion object {
        const val PREFIX = "prop"

        val SPECIAL_CASES = listOf(
            SpecialCaseBooleanResolver(),
            SpecialCaseNullResolver(),
            SpecialCaseThisResolver(),
            SpecialCaseLiteralStringResolver(),
            SpecialCaseNumericResolver(),
            SpecialCaseRangeIntegersResolver()
        )

        val NORMAL_CASES = listOf(SingleMethodResolver(), SinglePropertyResolver())
    }
}
