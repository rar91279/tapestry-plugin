package com.github.rar91279.plugin.tapestry.intellij.core.resource.xml

import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlAttribute
import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlTag

/** [XmlAttribute] backed by an IntelliJ PSI attribute. */
class IntellijXmlAttribute(private val xmlAttribute: com.intellij.psi.xml.XmlAttribute) : XmlAttribute {

    override val name: String
        get() = xmlAttribute.name

    override val localName: String
        get() = xmlAttribute.localName

    override val namespace: String
        get() = xmlAttribute.namespace

    override val value: String?
        get() = xmlAttribute.value
}

/** [XmlTag] backed by an IntelliJ PSI tag. */
class IntellijXmlTag(private val xmlTag: com.intellij.psi.xml.XmlTag) : XmlTag {

    override val name: String
        get() = xmlTag.name

    override val namespace: String
        get() = xmlTag.namespace

    override val localName: String
        get() = xmlTag.localName

    override val text: String
        get() = xmlTag.text

    override val attributes: Array<XmlAttribute>
        get() = xmlTag.attributes.map { IntellijXmlAttribute(it) }.toTypedArray()

    override fun getAttribute(name: String, namespace: String): XmlAttribute? =
        xmlTag.getAttribute(name, namespace)?.let { IntellijXmlAttribute(it) }
}
