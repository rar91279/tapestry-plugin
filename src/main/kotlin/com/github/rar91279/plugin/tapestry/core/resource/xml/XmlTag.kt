package com.github.rar91279.plugin.tapestry.core.resource.xml

/**
 * Represents a XML tag.
 */
interface XmlTag {

    val name: String?

    val namespace: String?

    /** The local name of the tag (without the namespace). */
    val localName: String?

    val text: String?

    val attributes: Array<XmlAttribute>

    /**
     * @return the attribute with the given local name in the given namespace, `null` if none is found.
     */
    fun getAttribute(name: String, namespace: String): XmlAttribute?
}
