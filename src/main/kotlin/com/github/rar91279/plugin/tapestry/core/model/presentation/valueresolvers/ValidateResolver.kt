package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.intellij.psi.PsiTypes

/** Resolves validate values. */
class ValidateResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("validate") { PsiTypes.nullType() }
}
