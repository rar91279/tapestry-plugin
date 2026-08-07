package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.github.rar91279.plugin.tapestry.core.java.AssignableToAll

/** Resolves validate values. */
class ValidateResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("validate") { AssignableToAll }
}
