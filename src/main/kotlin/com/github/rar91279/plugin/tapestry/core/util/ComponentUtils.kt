package com.github.rar91279.plugin.tapestry.core.util

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlAttribute
import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlTag
import com.github.rar91279.plugin.tapestry.intellij.lang.descriptor.TapestryXmlExtension

/**
 * Utility methods related to Tapestry components.
 */ // todo remove it
object ComponentUtils {

    /**
     * @return `true` if the given tag is an opening or closing tag of a Tapestry component.
     */
    @JvmStatic
    fun isComponentTag(tag: XmlTag): Boolean =
        isTapestryNamespace(tag.namespace) || hasTapestryNamespaceAttribute(tag.attributes)

    private fun hasTapestryNamespaceAttribute(attributes: Array<XmlAttribute>): Boolean =
        attributes.any { !it.localName.isNullOrEmpty() && isTapestryNamespace(it.namespace) }

    private fun isTapestryNamespace(namespace: String?): Boolean =
        TapestryXmlExtension.isTapestryTemplateNamespace(namespace) || namespace == TapestryConstants.PARAMETERS_NAMESPACE
}
