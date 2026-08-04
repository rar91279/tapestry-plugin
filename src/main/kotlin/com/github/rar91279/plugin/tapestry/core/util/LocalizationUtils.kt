package com.github.rar91279.plugin.tapestry.core.util

import java.util.Locale

/**
 * Utility methods related to resource localization.
 */
object LocalizationUtils {

    private val ALL_COUNTRIES = Locale.getISOCountries().toSet()
    private val ALL_LANGUAGES = Locale.getISOLanguages().toSet()

    /**
     * Finds the not localized name of a file. For example:
     *
     * `Somefile_en.properties` -> `Somefile.properties`,
     * `Somefile_en_GB.properties` -> `Somefile.properties`,
     * `Somefile.properties` -> `Somefile.properties`
     */
    @JvmStatic
    fun unlocalizeFileName(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        val extension = if (lastDot >= 0) lastDot else filename.length
        val lastUnderscore = filename.lastIndexOf('_', extension)
        if (lastUnderscore < 1) return filename

        val languageOrCountry = filename.substring(lastUnderscore + 1, extension)

        // Last token is the language ?
        if (languageOrCountry in ALL_LANGUAGES) return removeSubstring(filename, lastUnderscore, extension)

        // Last token is the country ?
        if (languageOrCountry !in ALL_COUNTRIES) return filename

        val nextUnderscore = filename.lastIndexOf('_', lastUnderscore - 1)
        if (nextUnderscore < 1) return filename

        val language = filename.substring(nextUnderscore + 1, lastUnderscore)
        return if (language in ALL_LANGUAGES) removeSubstring(filename, nextUnderscore, extension) else filename
    }

    private fun removeSubstring(filename: String, beginIndex: Int, endIndex: Int): String =
        filename.substring(0, beginIndex) + filename.substring(endIndex)
}
