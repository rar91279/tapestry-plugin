package com.intellij.tapestry.lang

import com.intellij.lang.xml.XMLLanguage

/**
 * @author Alexey Chmutov
 */
object TmlLanguage : XMLLanguage(INSTANCE, "TML", "application/xml", "text/xml") {
    private fun readResolve(): Any = TmlLanguage
}
