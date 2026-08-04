package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class DocNavSpec : FreeSpec({
    val nav = DocNav

    "js escapes chars that break a single-quoted JS string inside an HTML attribute" {
        nav.js("module/John's App") shouldBe "module/John\\'s App"
        nav.js("a\\b") shouldBe "a\\\\b"
        nav.js("x&y\"<z") shouldBe "x&amp;y&quot;&lt;z"
    }

    "js leaves plain tokens and null untouched" {
        nav.js("el/app/pages/admin/UserList") shouldBe "el/app/pages/admin/UserList"
        nav.js(null) shouldBe ""
    }
})
