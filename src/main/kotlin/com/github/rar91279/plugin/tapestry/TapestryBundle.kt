/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rar91279.plugin.tapestry

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

