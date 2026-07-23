package com.intellij.tapestry.core.model.presentation

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk

// TemplateElement's first constructor parameter is annotated @NotNull, so a plain `null` literal
// is rejected by the Kotlin compiler. These tests deliberately pass null, so launder it through
// an erased type parameter (no runtime null check inserted).
@Suppress("UNCHECKED_CAST")
private fun <T> nullValue(): T = null as T

class TemplateElementTest : FreeSpec({

    "constructor_with_null_values" {
        val templateElement = TemplateElement(nullValue(), null)

        templateElement.element shouldBe null

        templateElement.template shouldBe null
    }

    "constructor_with_some_null_values" {
        val templateMock = "template1"
        val injectedElement = mockk<InjectedElement>(relaxed = true)

        val templateElement = TemplateElement(nullValue(), templateMock)

        templateElement.element shouldBe null

        templateElement.template shouldBe templateMock

        val templateElement2 = TemplateElement(injectedElement, null)

        templateElement2.element shouldBe injectedElement

        templateElement2.template shouldBe null
    }

    "compareTo" - {
        val templateMock = "template1"
        val templateMock2 = "template2"
        val injectedElement = mockk<InjectedElement>(relaxed = true)
        "equal to himself"{
            TemplateElement(injectedElement, templateMock) shouldBeEqualComparingTo TemplateElement(injectedElement, templateMock)
        }

        "lesser than other"{
            TemplateElement(injectedElement, templateMock) shouldBeLessThan TemplateElement(injectedElement, templateMock2)
        }
    }
})
