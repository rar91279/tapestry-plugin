package com.intellij.tapestry.core.util

import com.intellij.tapestry.core.resource.TestableResource
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class WebDescriptorUtilsTest : FreeSpec({

    lateinit var document1: Document
    lateinit var document2: Document
    lateinit var document5: Document
    lateinit var document6: Document

    beforeTest {
        val documentBuilder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder()

        fun parse(resource: String): Document =
            documentBuilder.parse(File(TestableResource::class.java.getResource(resource)!!.toURI()))

        document1 = parse("/web/web1.xml")
        document2 = parse("/web/web2.xml")
        document5 = parse("/web/web5.xml")
        document6 = parse("/web/web6.xml")
    }

    "constructor is callable" {
        WebDescriptorUtils()
    }

    "getTapestryFilterName" - {
        "not_found" {
            WebDescriptorUtils.getTapestryFilterName(document5) shouldBe null
            WebDescriptorUtils.getTapestryFilterName(document6) shouldBe null
        }

        "found" {
            WebDescriptorUtils.getTapestryFilterName(document1) shouldBe "app"
        }
    }

    "getApplicationPackage" - {
        "not_found" {
            WebDescriptorUtils.getApplicationPackage(document2) shouldBe null
            WebDescriptorUtils.getApplicationPackage(document6) shouldBe null
        }

        "found" {
            WebDescriptorUtils.getApplicationPackage(document1) shouldBe "org.example.myapp"
        }
    }
})
