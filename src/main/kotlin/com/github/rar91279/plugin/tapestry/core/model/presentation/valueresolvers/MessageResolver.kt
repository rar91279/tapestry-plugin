package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

/** Resolves message values. */
class MessageResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("message") { context.findType("java.lang.String") }
}
