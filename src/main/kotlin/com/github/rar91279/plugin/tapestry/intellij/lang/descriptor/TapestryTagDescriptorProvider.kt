package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.openapi.project.DumbService
import com.intellij.psi.impl.source.xml.XmlElementDescriptorProvider
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.psi.TmlFile
import com.intellij.xml.XmlElementDescriptor

/** Provides the element descriptors of the tags in a Tapestry template. */
class TapestryTagDescriptorProvider : XmlElementDescriptorProvider {

    override fun getDescriptor(tag: XmlTag): XmlElementDescriptor? {
        if (DumbService.isDumb(tag.project)) return null

        return if (tag.containingFile is TmlFile) DescriptorUtil.getTmlOrHtmlTagDescriptor(tag) else null
    }
}
