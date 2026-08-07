package com.github.rar91279.plugin.tapestry.intellij.core.resource.xml

import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlAttribute

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
