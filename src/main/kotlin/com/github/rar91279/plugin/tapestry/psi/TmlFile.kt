package com.github.rar91279.plugin.tapestry.psi

import com.intellij.psi.FileViewProvider
import com.intellij.psi.impl.source.xml.XmlFileImpl
import com.intellij.psi.xml.XmlFile

/** A Tapestry template file. */
class TmlFile(viewProvider: FileViewProvider) : XmlFileImpl(viewProvider, TmlElementType.TML_FILE), XmlFile {

    override fun toString(): String = "TmlFile:$name"
}
