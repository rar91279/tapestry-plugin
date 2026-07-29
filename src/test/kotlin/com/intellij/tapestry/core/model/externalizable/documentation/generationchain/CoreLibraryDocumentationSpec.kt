package com.intellij.tapestry.core.model.externalizable.documentation.generationchain

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class CoreLibraryDocumentationSpec : FreeSpec({

    "render" - {
        "returns null when no descriptor exists for the element" {
            CoreLibraryDocumentation.render("components", "NoSuchComponent") shouldBe null
        }

        "renders the element page from a bundled descriptor" {
            val html = CoreLibraryDocumentation.render("components", "Checkbox")
            html.shouldNotBeNull()
            html shouldContain "Description"   // structural: the core-element template rendered
        }
    }

    "renderIndex" - {
        val html = CoreLibraryDocumentation.renderIndex()

        "titles the page and lists the three element sections" {
            html shouldContain "Core Library"
            html shouldContain ">Pages<"
            html shouldContain ">Components<"
            html shouldContain ">Mixins<"
        }

        "lists bundled elements as clickable navigation entries" {
            html shouldContain "tapestryNav('core/components/Checkbox')"
            html shouldContain ">Checkbox<"
        }

        "sorts entries within a section case-insensitively by name" {
            val first = html.indexOf("core/components/ActionLink")
            val last = html.indexOf("core/components/Zone")
            first shouldBeGreaterThanOrEqual 0
            first shouldBeLessThan last
        }
    }
})
