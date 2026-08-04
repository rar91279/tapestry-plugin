package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.intellij.openapi.util.text.StringUtil
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseBooleanResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseLiteralStringResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseNullResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseNumericResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseRangeIntegersResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases.SpecialCaseThisResolver
import com.github.rar91279.plugin.tapestry.core.util.ClassUtils
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

/**
 * Resolves the special case when a property value is given as a method name.
 */
class SingleMethodResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        val cleanValue = getCleanValue(context.value) ?: return false
        if (!PATTERN.matches(cleanValue)) return false

        val methodName = cleanValue.substringBefore("()")
        val method = context.contextClass?.findPublicMethods(methodName)?.find { it.parameters.isEmpty() }

        if (method != null) {
            context.resultType = method.returnType
            if (method.returnType != null) context.resultCodeBind = method
        }

        return true
    }

    private companion object {
        val PATTERN = Regex("\\w+\\(\\)")
    }
}

/**
 * Resolves the special case when a property value is given as a property name.
 */
class SinglePropertyResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        val cleanValue = getCleanValue(context.value) ?: return false
        if (!PATTERN.matches(cleanValue)) return false

        val properties = ClassUtils.getClassProperties(context.contextClass)

        for ((name, boundTo) in properties) {
            if (!StringUtil.toLowerCase(name).equals(StringUtil.toLowerCase(cleanValue))) continue

            context.resultType = when (boundTo) {
                is IJavaMethod -> boundTo.returnType
                is IJavaField -> boundTo.type
                else -> continue
            }
            context.resultCodeBind = boundTo

            return true
        }

        return true
    }

    private companion object {
        val PATTERN = Regex("[a-zA-Z\$_][a-zA-Z0-9\$_.]*")
    }
}
