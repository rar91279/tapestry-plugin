package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.PropResolver
import com.github.rar91279.plugin.tapestry.core.util.chain.ChainBase

/**
 * The starting point of the resolvers chain.
 */
object ValueResolverChain : ChainBase(
    arrayOf(PropResolver(), LiteralResolver(), ComponentResolver(), ValidateResolver(), MessageResolver())
) {

    private val logger = Logger.getInstance(ValueResolverChain::class.java)

    /**
     * Resolves a value.
     *
     * @return the resolved value or `null` if it wasn't possible to resolve the value.
     * @throws Exception if an error occurs resolving the value.
     */
    @Throws(Exception::class)
    fun resolve(
        project: TapestryProject,
        contextClass: IJavaClassType?,
        value: String?,
        defaultPrefix: String?
    ): ResolvedValue? {
        val context = ValueResolverContext(project, contextClass, value, defaultPrefix)

        try {
            execute(context)
        } catch (ex: Exception) {
            if (ex !is ProcessCanceledException) logger.error(ex)
            throw ex
        }

        return context.resultType?.let { ResolvedValue(it, context.resultCodeBind) }
    }
}
