package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.intellij.psi.PsiClass
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.AbstractValueResolver.Companion.resolveWith
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.PropResolver

/**
 * The starting point of value resolution: the first resolver that handles the value wins.
 */
object ValueResolverChain {

    private val logger = Logger.getInstance(ValueResolverChain::class.java)

    private val resolvers = listOf(
        PropResolver(), LiteralResolver(), ComponentResolver(), ValidateResolver(), MessageResolver()
    )

    /**
     * Resolves a value.
     *
     * @return the resolved value or `null` if it wasn't possible to resolve the value.
     * @throws Exception if an error occurs resolving the value.
     */
    @Throws(Exception::class)
    fun resolve(
        project: TapestryProject,
        contextClass: PsiClass?,
        value: String?,
        defaultPrefix: String?
    ): ResolvedValue? {
        val context = ValueResolverContext(project, contextClass, value, defaultPrefix)

        try {
            resolvers.resolveWith(context)
        } catch (ex: Exception) {
            if (ex !is ProcessCanceledException) logger.error(ex)
            throw ex
        }

        return context.resultType?.let { ResolvedValue(it, context.resultCodeBind) }
    }
}
