package com.github.rar91279.plugin.tapestry.core.resource

import java.io.File
import java.net.URISyntaxException

class TestableResource(override val name: String, private val _fileName: String) : IResource {

    override val file: File?
        get() = try {
            val url = TestableResource::class.java.getResource("/web/$_fileName")
            if (url != null) File(url.toURI()) else null
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            null
        }

    override val extension: String
        get() = _fileName.substring(_fileName.lastIndexOf('.') + 1, _fileName.length - 1)

    override fun accept(visitor: CoreXmlRecursiveElementVisitor) {
    }
}
