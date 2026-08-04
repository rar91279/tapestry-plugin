package com.intellij.tapestry.core.resource.xml

/**
 * A XML tag attribute.
 */
interface XmlAttribute {

    val name: String?

    /** The localname of the attribute (without the namespace). */
    val localName: String?

    val namespace: String?

    val value: String?
}

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
