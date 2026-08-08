package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.util.ClassUtils
import com.github.rar91279.plugin.tapestry.core.util.erasedIfTypeVariable

/**
 * Resolves the special case when a property value is given as a property name.
 */
class SinglePropertyResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        val cleanValue = getCleanValue(context.value) ?: return false
        if (!PATTERN.matches(cleanValue)) return false

        val properties = ClassUtils.getClassProperties(context.contextClass)

        for ((name, boundTo) in properties) {
            if (name.lowercase() != cleanValue.lowercase()) continue

            context.resultType = when (boundTo) {
                is PsiMethod -> boundTo.returnType?.erasedIfTypeVariable()
                is PsiField -> boundTo.type.erasedIfTypeVariable()
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
