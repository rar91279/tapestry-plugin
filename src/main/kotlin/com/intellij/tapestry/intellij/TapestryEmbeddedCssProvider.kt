package com.intellij.tapestry.intellij

import com.intellij.lang.Language
import com.intellij.psi.css.EmbeddedCssProvider
import com.intellij.tapestry.lang.TmlLanguage

class TapestryEmbeddedCssProvider : EmbeddedCssProvider() {
    override fun enableEmbeddedCssFor(language: Language) = language.`is`(TmlLanguage)
}
