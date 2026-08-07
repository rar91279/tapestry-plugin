package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.util.chain.Command
import com.github.rar91279.plugin.tapestry.core.util.chain.Context

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
