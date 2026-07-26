/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.tapestry

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.TapestryBundle"

/**
 * Message bundle for the Tapestry plugin.
 *
 * Provides access to localized strings defined in the `messages.TapestryBundle` resource bundle.
 * This object is used throughout the Tapestry plugin for internationalization (i18n) of user-facing text.
 */
object TapestryBundle : DynamicBundle(BUNDLE) {

    /**
     * Retrieves a localized message from the bundle by its key.
     *
     * @param key the property key identifying the message in the resource bundle
     * @param params optional parameters to substitute into the message template
     * @return the localized and formatted message string
     */
    @JvmStatic
    @Nls
    fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any
    ) = getMessage(key, *params)

    /**
     * Creates a lazy message supplier for deferred localization.
     *
     * Returns a supplier that will retrieve and format the message when accessed,
     * allowing for dynamic language changes and reduced initialization overhead.
     *
     * @param key the property key identifying the message in the resource bundle
     * @param params optional parameters to substitute into the message template
     * @return a lazy message supplier
     */
    fun messagePointer(
        @PropertyKey(resourceBundle = BUNDLE) key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any
    ) = getLazyMessage(key, *params)
}
