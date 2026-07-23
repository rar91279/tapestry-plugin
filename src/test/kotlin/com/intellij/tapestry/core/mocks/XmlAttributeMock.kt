package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.resource.xml.XmlAttribute

/**
 * Utility class for easy creation of XmlAttribute mocks.
 */
class XmlAttributeMock() : XmlAttribute {

    private var _name: String? = null
    private var _localName: String? = null
    private var _namespace: String? = null
    private var _value: String? = null

    constructor(localName: String?, value: String?) : this() {
        _localName = localName
        _value = value
    }

    override fun getName(): String? = _name

    fun setName(name: String?): XmlAttributeMock {
        _name = name
        return this
    }

    override fun getLocalName(): String? = _localName

    fun setLocalName(localName: String?): XmlAttributeMock {
        _localName = localName
        return this
    }

    override fun getNamespace(): String? = _namespace

    fun setNamespace(namespace: String?): XmlAttributeMock {
        _namespace = namespace
        return this
    }

    override fun getValue(): String? = _value

    fun setValue(value: String?): XmlAttributeMock {
        _value = value
        return this
    }
}
