package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.github.rar91279.plugin.tapestry.core.java.AssignableToAll

/** Resolves component values. */
class ComponentResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("component") { AssignableToAll }
}
