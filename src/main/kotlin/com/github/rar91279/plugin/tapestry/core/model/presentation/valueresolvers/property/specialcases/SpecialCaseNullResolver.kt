package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.intellij.psi.CommonClassNames
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext

/** Resolves the special case when a property value is given as a null literal. */
class SpecialCaseNullResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean {
        if (context.cleanValueLowercased() != "null") return false

        context.resultType = context.findType(CommonClassNames.JAVA_LANG_OBJECT)
        return true
    }
}
