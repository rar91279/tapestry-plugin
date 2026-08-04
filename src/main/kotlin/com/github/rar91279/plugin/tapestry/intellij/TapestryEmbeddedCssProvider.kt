package com.github.rar91279.plugin.tapestry.intellij

import com.intellij.lang.Language
import com.intellij.psi.css.EmbeddedCssProvider
import com.github.rar91279.plugin.tapestry.lang.TmlLanguage

class TapestryEmbeddedCssProvider : EmbeddedCssProvider() {
    override fun enableEmbeddedCssFor(language: Language) = language.`is`(TmlLanguage)
}
