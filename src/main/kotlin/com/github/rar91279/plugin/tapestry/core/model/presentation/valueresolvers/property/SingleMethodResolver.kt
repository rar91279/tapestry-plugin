package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.github.rar91279.plugin.tapestry.core.util.erasedIfTypeVariable
import com.github.rar91279.plugin.tapestry.core.util.findPublicMethods
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext

/**
 * Resolves the special case when a property value is given as a method name.
 */
class SingleMethodResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        val cleanValue = getCleanValue(context.value) ?: return false
        if (!PATTERN.matches(cleanValue)) return false

        val methodName = cleanValue.substringBefore("()")
        val method = context.contextClass?.findPublicMethods(methodName)?.find { it.parameterList.isEmpty }

        if (method != null) {
            context.resultType = method.returnType?.erasedIfTypeVariable()
            if (method.returnType != null) context.resultCodeBind = method
        }

        return true
    }

    private companion object {
        val PATTERN = Regex("\\w+\\(\\)")
    }
}
