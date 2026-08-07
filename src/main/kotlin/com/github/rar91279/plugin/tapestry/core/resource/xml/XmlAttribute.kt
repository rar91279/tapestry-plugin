package com.github.rar91279.plugin.tapestry.core.resource.xml

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
