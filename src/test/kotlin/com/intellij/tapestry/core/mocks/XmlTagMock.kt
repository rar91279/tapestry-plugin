package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.resource.xml.XmlAttribute
import com.intellij.tapestry.core.resource.xml.XmlTag

/**
 * Utility class for easy creation of XmlTag mocks.
 */
class XmlTagMock() : XmlTag {

    override var name: String? = null
    override var namespace: String? = null
    override var localName: String? = null
    override var text: String? = null

    private val _attributes = ArrayList<XmlAttribute>()

    constructor(localName: String?) : this() {
        this.localName = localName
    }

    fun setName(name: String?): XmlTagMock {
        this.name = name
        return this
    }

    fun setNamespace(namespace: String?): XmlTagMock {
        this.namespace = namespace
        return this
    }

    fun setLocalName(localName: String?): XmlTagMock {
        this.localName = localName
        return this
    }

    fun setText(text: String?): XmlTagMock {
        this.text = text
        return this
    }

    override val attributes: Array<XmlAttribute>
        get() = _attributes.toTypedArray()

    fun addAttribute(attribute: XmlAttribute): XmlTagMock {
        _attributes.add(XmlAttributeMock(attribute.localName, attribute.value))
        return this
    }

    override fun getAttribute(name: String, namespace: String): XmlAttribute? = null
}
