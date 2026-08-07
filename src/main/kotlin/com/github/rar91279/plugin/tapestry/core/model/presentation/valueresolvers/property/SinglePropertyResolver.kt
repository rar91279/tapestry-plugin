package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.intellij.openapi.util.text.StringUtil
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.util.ClassUtils

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
