package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

/** Resolves literal values. */
class LiteralResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("literal") { context.findType("java.lang.String") }
}
