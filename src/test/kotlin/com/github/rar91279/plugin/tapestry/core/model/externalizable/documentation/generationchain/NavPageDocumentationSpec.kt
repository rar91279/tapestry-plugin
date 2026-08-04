package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class NavPageDocumentationSpec : FreeSpec({

    "summary" - {
        "returns empty string for null" {
            NavPageDocumentation.summary(null) shouldBe ""
        }

        "strips markup and collapses whitespace" {
            NavPageDocumentation.summary("<p>Hello   <b>world</b></p>") shouldBe "Hello world"
        }

        "trims surrounding whitespace" {
            NavPageDocumentation.summary("  spaced text  ") shouldBe "spaced text"
        }

        "leaves text at or below 120 chars untouched" {
            val text = "x".repeat(120)
            NavPageDocumentation.summary(text) shouldBe text
        }

        "truncates a long unbroken run at 120 chars with an ellipsis" {
            NavPageDocumentation.summary("a".repeat(130)) shouldBe "a".repeat(120) + "…"
        }

        "truncates a long string at the last word boundary before 120 chars" {
            // 50 "ab" words (149 chars) → last space at index 119 → first 40 words kept.
            val input = "ab ".repeat(50).trim()
            NavPageDocumentation.summary(input) shouldBe "ab ".repeat(40).trim() + "…"
        }
    }

    "render" - {
        val section = NavPageDocumentation.Section("Pages", listOf(
            NavPageDocumentation.Entry("Home", "el/app/pages/Home", "The start page", "eager"),
            NavPageDocumentation.Entry("Plain", "", "")))

        "emits the title, clickable entries, badges and section headers" {
            val html = NavPageDocumentation.render("My Title", "1.2.3", "pom/g/a", listOf(section))

            html shouldContain "My Title"
            html shouldContain "1.2.3"
            html shouldContain "tapestryNav('pom/g/a')"      // subtitle is a link when token is set
            html shouldContain "tapestryNav('el/app/pages/Home')"
            html shouldContain ">Home<"
            html shouldContain "The start page"
            html shouldContain "tag-on\">eager"              // badge rendered
            html shouldContain ">Pages<"                     // section header
            html shouldContain "#s1"                         // menubar anchor
        }

        "renders an entry with no token as plain, non-clickable text" {
            val html = NavPageDocumentation.render("T", "", "", listOf(section))
            html shouldContain "Plain"
            html shouldNotContain "tapestryNav('')"          // empty token → no link
        }

        "escapes navigation tokens through DocNav.js" {
            val html = NavPageDocumentation.render("T", "", "",
                listOf(NavPageDocumentation.Section("S",
                    listOf(NavPageDocumentation.Entry("L", "module/John's App", "")))))
            html shouldContain "tapestryNav('module/John\\'s App')"
        }

        "renders 'None.' for an empty section" {
            val html = NavPageDocumentation.render("T", listOf(NavPageDocumentation.Section("Empty", emptyList())))
            html shouldContain "None."
        }

        "the two-arg overload emits no subtitle" {
            // No sections → the only place an 'anchor' span could appear is a subtitle; assert none.
            val html = NavPageDocumentation.render("Just A Title", emptyList())
            html shouldContain "Just A Title"
            html shouldNotContain "class=\"anchor\""
        }

        "treats a null title as empty without failing" {
            NavPageDocumentation.render(null, emptyList()) shouldNotContain "null"
        }
    }
})
