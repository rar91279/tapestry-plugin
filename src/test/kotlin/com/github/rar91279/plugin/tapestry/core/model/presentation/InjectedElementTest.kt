package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.mocks.psiAnnotationMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiFieldMock
import com.github.rar91279.plugin.tapestry.core.mocks.xmlAttributeMock
import com.github.rar91279.plugin.tapestry.core.mocks.xmlTagMock
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.intellij.psi.PsiField
import com.intellij.psi.xml.XmlTag
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

// The InjectedElement constructors annotate the first parameter @NotNull, so a plain `null`
// literal is rejected by the Kotlin compiler. These tests deliberately exercise null handling,
// so launder null through an erased type parameter (no runtime null check inserted).
@Suppress("UNCHECKED_CAST")
private fun <T> nullValue(): T = null as T

class InjectedElementTest : FreeSpec({

    "constructor_with_field" {
        val injectedElement = InjectedElement(nullValue<PsiField>(), null)

        injectedElement.element shouldBe null

        injectedElement.field shouldBe null
    }

    "constructor_with_tag" {
        val injectedElement = InjectedElement(nullValue<XmlTag>(), null)

        injectedElement.element shouldBe null

        injectedElement.tag shouldBe null
    }

    "getElementId_component_without_id_and_tag_null" {
        val fieldMock = psiFieldMock("field1", annotations = listOf(psiAnnotationMock(TapestryConstants.COMPONENT_ANNOTATION)))

        val componentMock = mockk<TapestryComponent>(relaxed = true)
        val injectedElement = InjectedElement(fieldMock, componentMock)

        injectedElement.elementId shouldBe "field1"
    }

    "getElementId_component_without_id_and_field_null" {
        val tagMock = xmlTagMock("tag1")

        val componentClassMock = psiClassMock("com.app.components.SomeComponent", isPublic = true)
        val tapestryProjectMock = mockk<TapestryProject>(relaxed = true)
        val libraryMock = TapestryLibrary("id", "com.app", tapestryProjectMock)

        val componentMock = TapestryComponent(libraryMock, componentClassMock, tapestryProjectMock)

        val injectedElement = InjectedElement(tagMock, componentMock)

        injectedElement.elementId shouldBe "SomeComponent"

        val tagMock2 = xmlTagMock("someComponent")

        val injectedElement2 = InjectedElement(tagMock2, componentMock)

        injectedElement2.elementId shouldBe "someComponent"
    }

    "getElementId_component_with_id_and_field_null" {
        val tagMock = xmlTagMock("tag1", attributes = arrayOf(xmlAttributeMock("id", "tag2")))

        val componentMock = mockk<TapestryComponent>(relaxed = true)
        val injectedElement = InjectedElement(tagMock, componentMock)

        injectedElement.elementId shouldBe "tag2"
    }

    "getElementId_component_with_id_and_tag_null" {
        val fieldMock = psiFieldMock("field1", annotations = listOf(psiAnnotationMock(TapestryConstants.COMPONENT_ANNOTATION, "id" to listOf("field2"))))

        val componentMock = mockk<TapestryComponent>(relaxed = true)
        val injectedElement = InjectedElement(fieldMock, componentMock)

        injectedElement.elementId shouldBe "field2"
    }

    "getElementId_null_values" {
        val injectedElementWithField = InjectedElement(nullValue<PsiField>(), null)

        injectedElementWithField.elementId shouldBe null

        val injectedElementWithTag = InjectedElement(nullValue<XmlTag>(), null)

        injectedElementWithTag.elementId shouldBe null
    }

    "getParameters_with_null_values" {
        val componentMock = mockk<TapestryComponent>(relaxed = true)

        val injectedElement = InjectedElement(nullValue<PsiField>(), componentMock)
        injectedElement.parameters.size shouldBe 0

        val injectedElement2 = InjectedElement(nullValue<XmlTag>(), componentMock)
        injectedElement2.parameters.size shouldBe 0
    }

    "getParameters_without_null_values" {
        val values = listOf("id=field2")
        val fieldMock = psiFieldMock("field1", annotations = listOf(psiAnnotationMock(TapestryConstants.COMPONENT_ANNOTATION, "parameters" to values)))

        val tagMock = xmlTagMock("tag1", attributes = arrayOf(xmlAttributeMock("id", "tag2")))
        val componentMock = mockk<TapestryComponent>(relaxed = true)

        val injectedElement = InjectedElement(fieldMock, componentMock)
        injectedElement.parameters.size shouldBe 1

        val injectedElement2 = InjectedElement(tagMock, componentMock)
        injectedElement2.parameters.size shouldBe 1
    }

    "compareTo" {
        val fieldMock = psiFieldMock("field1")
        val fieldMock2 = psiFieldMock("field2")
        val componentMock = mockk<TapestryComponent>(relaxed = true)

        InjectedElement(fieldMock, componentMock).compareTo(InjectedElement(fieldMock, componentMock)) shouldBe 0

        (InjectedElement(fieldMock, componentMock).compareTo(InjectedElement(fieldMock2, componentMock)) < 0) shouldBe true
    }
})
