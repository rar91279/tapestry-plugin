package com.github.rar91279.plugin.tapestry.psi

import com.intellij.psi.tree.IFileElementType
import com.github.rar91279.plugin.tapestry.lang.TmlLanguage

/** The file element type of a Tapestry template. */
object TmlElementType {

    @JvmField
    val TML_FILE: IFileElementType = IFileElementType("TML_FILE", TmlLanguage)
}
