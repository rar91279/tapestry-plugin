package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import com.intellij.psi.PsiType

/**
 * Base class for all value resolvers.
 *
 * Resolvers are combined by plain `List<AbstractValueResolver>` + [resolveWith]; there used to be a
 * four-type chain-of-responsibility framework here, whose only effect was to erase the context type and
 * force every resolver to cast it back.
 */
abstract class AbstractValueResolver {

    /** @return `true` if this resolver handled the value and the search should stop. */
    @Throws(Exception::class)
    abstract fun resolve(context: ValueResolverContext): Boolean

    /** Resolves to [type] when the context value carries exactly [prefix]. */
    protected fun ValueResolverContext.resolveIfPrefix(prefix: String, type: () -> PsiType?): Boolean {
        if (getPrefix(value, defaultPrefix) != prefix) return false

        resultType = type()
        return true
    }

    protected fun ValueResolverContext.findType(fullyQualifiedName: String): PsiType? =
        project.findClassType(fullyQualifiedName)

    /** The clean value, trimmed and lowercased, as the special case resolvers match on it. */
    protected fun ValueResolverContext.cleanValueLowercased(): String? = getCleanValue(value)?.trim()?.lowercase()

    companion object {

        /** Runs the resolvers in order, stopping at the first one that handles [context]. */
        @Throws(Exception::class)
        fun List<AbstractValueResolver>.resolveWith(context: ValueResolverContext): Boolean =
            any { it.resolve(context) }

        /**
         * Trims and removes the prefix of the value.
         */
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
