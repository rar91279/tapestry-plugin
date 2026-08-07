package com.github.rar91279.plugin.tapestry.core.resource

import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlTag

/**
 * A visitor for XML files.
 */
interface CoreXmlRecursiveElementVisitor {

    fun visitTag(tag: XmlTag)
}
