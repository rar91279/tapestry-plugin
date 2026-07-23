package com.intellij.tapestry.core.resource

import java.io.File
import java.net.URISyntaxException

class TestableResource(private val _name: String, private val _fileName: String) : IResource {

    override fun getName(): String = _name

    override fun getFile(): File? {
        return try {
            val url = TestableResource::class.java.getResource("/web/$_fileName")
            if (url != null) File(url.toURI()) else null
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            null
        }
    }

    override fun getExtension(): String =
        _fileName.substring(_fileName.lastIndexOf('.') + 1, _fileName.length - 1)

    override fun accept(visitor: CoreXmlRecursiveElementVisitor) {
    }
}
