package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.resource.xml.XmlAttribute
import com.intellij.tapestry.core.resource.xml.XmlTag

/**
 * Utility class for easy creation of XmlTag mocks.
 */
class XmlTagMock() : XmlTag {

    private var _name: String? = null
    private var _namespace: String? = null
    private var _localName: String? = null
    private var _text: String? = null
    private val _attributes = ArrayList<XmlAttribute>()

    constructor(localName: String?) : this() {
        _localName = localName
    }

    override fun getName(): String? = _name

    fun setName(name: String?): XmlTagMock {
        _name = name
        return this
    }

    override fun getNamespace(): String? = _namespace

    fun setNamespace(namespace: String?): XmlTagMock {
        _namespace = namespace
        return this
    }

    override fun getLocalName(): String? = _localName

    fun setLocalName(localName: String?): XmlTagMock {
        _localName = localName
        return this
    }

    override fun getText(): String? = _text

    fun setText(text: String?): XmlTagMock {
        _text = text
        return this
    }

    override fun getAttributes(): Array<XmlAttribute> = _attributes.toTypedArray()

    fun addAttribute(attribute: XmlAttribute): XmlTagMock {
        _attributes.add(XmlAttributeMock(attribute.localName, attribute.value))
        return this
    }

    override fun getAttribute(name: String, namespace: String): XmlAttribute? = null
}
