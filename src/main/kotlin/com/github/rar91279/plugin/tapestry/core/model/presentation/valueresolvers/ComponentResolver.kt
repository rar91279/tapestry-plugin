package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.intellij.psi.PsiTypes

/** Resolves component values. */
class ComponentResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("component") { PsiTypes.nullType() }
}
