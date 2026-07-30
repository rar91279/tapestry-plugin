package com.intellij.tapestry.core.model.externalizable.documentation.generationchain

import com.intellij.tapestry.core.model.externalizable.documentation.Home
import com.intellij.tapestry.core.model.presentation.Mixin
import com.intellij.tapestry.core.model.presentation.Page
import com.intellij.tapestry.core.model.presentation.TapestryComponent
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk

/**
 * Characterizes the documentation generation entry point: which element type routes to which
 * template + icon. The per-type icon (expui/nodes/class|method|parameter.svg) is the routing
 * fingerprint.
 */
class DocumentationGeneratorSpec : FreeSpec({

    fun generate(element: Any): String =
        DocumentationGenerator.generate(element)!!

    "routes a component to the presentation template with the component icon" {
        val html = generate(mockk<TapestryComponent>(relaxed = true))
        html shouldContain "expui/nodes/class.svg"
        html shouldContain "Description"   // presentation-element.vm rendered
    }

    "routes a page to the presentation template with the page icon" {
        val html = generate(mockk<Page>(relaxed = true))
        html shouldContain "expui/nodes/parameter.svg"
        html shouldContain "Description"
    }

    "routes a mixin to the presentation template with the mixin icon" {
        val html = generate(mockk<Mixin>(relaxed = true))
        html shouldContain "expui/nodes/method.svg"
        html shouldContain "Description"
    }

    "routes a Home to the home template listing its modules" {
        val home = Home(listOf(Home.ModuleDoc("MyModule", true, emptyList())))
        val html = generate(home)
        html shouldContain "MyModule"
    }
})
