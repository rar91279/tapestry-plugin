package com.intellij.tapestry.core.mocks

import com.intellij.tapestry.core.resource.xml.XmlAttribute

/**
 * Utility class for easy creation of XmlAttribute mocks.
 */
class XmlAttributeMock() : XmlAttribute {

    override var name: String? = null
    override var localName: String? = null
    override var namespace: String? = null
    override var value: String? = null

    constructor(localName: String?, value: String?) : this() {
        this.localName = localName
        this.value = value
    }

    fun setName(name: String?): XmlAttributeMock {
        this.name = name
        return this
    }

    fun setLocalName(localName: String?): XmlAttributeMock {
        this.localName = localName
        return this
    }

    fun setNamespace(namespace: String?): XmlAttributeMock {
        this.namespace = namespace
        return this
    }

    fun setValue(value: String?): XmlAttributeMock {
        this.value = value
        return this
    }
}
