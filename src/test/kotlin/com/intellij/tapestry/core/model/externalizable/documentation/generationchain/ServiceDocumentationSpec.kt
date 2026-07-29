package com.intellij.tapestry.core.model.externalizable.documentation.generationchain

import com.intellij.tapestry.core.model.externalizable.documentation.Home
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ServiceDocumentationSpec : FreeSpec({

    "render" - {
        "emits id, a clickable class link, scope, eager-load flag and description" {
            val html = ServiceDocumentation.render(
                Home.ServiceDoc("MyService", "com.example.MyServiceImpl", "singleton", true, "Does the thing."))

            html shouldContain "MyService"
            html shouldContain "tapestryNav('class/com.example.MyServiceImpl')"
            html shouldContain "com.example.MyServiceImpl"
            html shouldContain ">Scope</td>"
            html shouldContain "singleton"
            html shouldContain "true"
            html shouldContain "Does the thing."
        }

        "shows 'unknown' and no class link when the class name is empty" {
            val html = ServiceDocumentation.render(
                Home.ServiceDoc("NoClass", "", "", false, ""))

            html shouldContain "unknown"
            html shouldNotContain "tapestryNav('class/"
        }

        "omits the scope row when scope is empty" {
            val html = ServiceDocumentation.render(
                Home.ServiceDoc("PlainService", "com.example.X", "", false, "desc"))
            html shouldNotContain ">Scope</td>"
        }

        "falls back to a placeholder when there is no description" {
            val html = ServiceDocumentation.render(
                Home.ServiceDoc("NoDoc", "com.example.X", "perthread", false, ""))
            html shouldContain "No documentation available for this service."
        }
    }
})
