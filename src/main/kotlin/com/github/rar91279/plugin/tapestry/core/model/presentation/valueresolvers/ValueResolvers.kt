package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.AssignableToAll
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.PropResolver
import com.github.rar91279.plugin.tapestry.core.util.chain.ChainBase
import com.github.rar91279.plugin.tapestry.core.util.chain.Command
import com.github.rar91279.plugin.tapestry.core.util.chain.Context

/** The resolved value. */
class ResolvedValue(val type: IJavaType?, val codeBind: Any?)

/** The state shared by the resolvers of a single resolution run. */
class ValueResolverContext(
    val project: TapestryProject,
    val contextClass: IJavaClassType?,
    val value: String?,
    val defaultPrefix: String?
) : Context {

    var resultType: IJavaType? = null
    var resultCodeBind: Any? = null
}

/**
 * Base class for all value resolver commands.
 */
abstract class AbstractValueResolver : Command {

    @Throws(Exception::class)
    final override fun execute(context: Context): Boolean = resolve(context as ValueResolverContext)

    @Throws(Exception::class)
    protected abstract fun resolve(context: ValueResolverContext): Boolean

    /** Resolves to [type] when the context value carries exactly [prefix]. */
    protected fun ValueResolverContext.resolveIfPrefix(prefix: String, type: () -> IJavaType?): Boolean {
        if (getPrefix(value, defaultPrefix) != prefix) return false

        resultType = type()
        return true
    }

    protected fun ValueResolverContext.findType(fullyQualifiedName: String): IJavaType? =
        project.javaTypeFinder.findType(fullyQualifiedName, true)

    /** The clean value, trimmed and lowercased, as the special case resolvers match on it. */
    protected fun ValueResolverContext.cleanValueLowercased(): String? = getCleanValue(value)?.trim()?.lowercase()

    companion object {

        /**
         * Trims and removes the prefix of the value.
         */
        @JvmStatic
        fun getCleanValue(value: String?): String? {
            if (value == null) return null

            var cleanValue = value.trim()

            if (cleanValue.startsWith("\${")) {
                val endIndex = cleanValue.lastIndexOf('}')
                cleanValue = (if (endIndex != -1) cleanValue.substring(2, endIndex) else cleanValue.substring(2)).trim()
            }

            val prefix = getPrefix(value, "") ?: return null

            return if (prefix.isEmpty()) cleanValue else cleanValue.substring(prefix.length + 1).trim()
        }

        /**
         * Finds the prefix in the value.
         *
         * @return the defined prefix in the value, the default prefix if no prefix was defined
         *         or `null` if the given value is invalid.
         */
        @JvmStatic
        fun getPrefix(value: String?, defaultPrefix: String?): String? {
            if (value == null) return null

            val colon = value.indexOf(':')
            if (colon == -1) return defaultPrefix

            val prefix = value.substring(0, colon)
            if (prefix.isEmpty()) return null

            return prefix.removePrefix("\${")
        }
    }
}

/** Resolves literal values. */
class LiteralResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("literal") { context.findType("java.lang.String") }
}

/** Resolves message values. */
class MessageResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("message") { context.findType("java.lang.String") }
}

/** Resolves component values. */
class ComponentResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("component") { AssignableToAll }
}

/** Resolves validate values. */
class ValidateResolver : AbstractValueResolver() {

    override fun resolve(context: ValueResolverContext): Boolean =
        context.resolveIfPrefix("validate") { AssignableToAll }
}

/**
 * The starting point of the resolvers chain.
 */
object ValueResolverChain : ChainBase(
    arrayOf(PropResolver(), LiteralResolver(), ComponentResolver(), ValidateResolver(), MessageResolver())
) {

    private val logger = Logger.getInstance(ValueResolverChain::class.java)

    @JvmStatic
    fun getInstance(): ValueResolverChain = this

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
