package com.intellij.tapestry.core.model.presentation

import com.intellij.tapestry.core.TapestryConstants
import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.mocks.JavaAnnotationMock
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.mocks.JavaFieldMock
import com.intellij.tapestry.core.mocks.XmlAttributeMock
import com.intellij.tapestry.core.mocks.XmlTagMock
import com.intellij.tapestry.core.model.TapestryLibrary
import com.intellij.tapestry.core.resource.xml.XmlTag
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
        val injectedElement = InjectedElement(nullValue<JavaFieldMock>(), null)

        injectedElement.getElement() shouldBe null

        injectedElement.getField() shouldBe null
    }

    "constructor_with_tag" {
        val injectedElement = InjectedElement(nullValue<XmlTag>(), null)

        injectedElement.getElement() shouldBe null

        injectedElement.getTag() shouldBe null
    }

    "getElementId_component_without_id_and_tag_null" {
        val fieldMock = JavaFieldMock("field1", true).addAnnotation(JavaAnnotationMock(TapestryConstants.COMPONENT_ANNOTATION))

        val componentMock = mockk<TapestryComponent>(relaxed = true)
        val injectedElement = InjectedElement(fieldMock, componentMock)

        injectedElement.getElementId() shouldBe "field1"
    }

    "getElementId_component_without_id_and_field_null" {
        val tagMock = XmlTagMock("tag1")

        val componentClassMock = JavaClassTypeMock("com.app.components.SomeComponent").setPublic(true).setDefaultConstructor(true)
        val tapestryProjectMock = mockk<TapestryProject>(relaxed = true)
        val libraryMock = TapestryLibrary("id", "com.app", tapestryProjectMock)

        val componentMock = TapestryComponent(libraryMock, componentClassMock, tapestryProjectMock)

        val injectedElement = InjectedElement(tagMock, componentMock)

        injectedElement.getElementId() shouldBe "SomeComponent"

        val tagMock2 = XmlTagMock("someComponent")

        val injectedElement2 = InjectedElement(tagMock2, componentMock)

        injectedElement2.getElementId() shouldBe "someComponent"
    }

    "getElementId_component_with_id_and_field_null" {
        val tagMock = XmlTagMock("tag1").addAttribute(XmlAttributeMock("id", "tag2"))

        val componentMock = mockk<TapestryComponent>(relaxed = true)
        val injectedElement = InjectedElement(tagMock, componentMock)

        injectedElement.getElementId() shouldBe "tag2"
    }

    "getElementId_component_with_id_and_tag_null" {
        val fieldMock = JavaFieldMock("field1", true).addAnnotation(JavaAnnotationMock(TapestryConstants.COMPONENT_ANNOTATION).addParameter("id", arrayOf("field2")))

        val componentMock = mockk<TapestryComponent>(relaxed = true)
        val injectedElement = InjectedElement(fieldMock, componentMock)

        injectedElement.getElementId() shouldBe "field2"
    }

    "getElementId_null_values" {
        val injectedElementWithField = InjectedElement(nullValue<JavaFieldMock>(), null)

        injectedElementWithField.getElementId() shouldBe null

        val injectedElementWithTag = InjectedElement(nullValue<XmlTag>(), null)

        injectedElementWithTag.getElementId() shouldBe null
    }

    "getParameters_with_null_values" {
        val componentMock = mockk<TapestryComponent>(relaxed = true)

        val injectedElement = InjectedElement(nullValue<JavaFieldMock>(), componentMock)
        injectedElement.getParameters().size shouldBe 0

        val injectedElement2 = InjectedElement(nullValue<XmlTag>(), componentMock)
        injectedElement2.getParameters().size shouldBe 0
    }

    "getParameters_without_null_values" {
        val values = arrayOf("id=field2")
        val fieldMock = JavaFieldMock("field1", true).addAnnotation(JavaAnnotationMock(TapestryConstants.COMPONENT_ANNOTATION).addParameter("parameters", values))

        val tagMock = XmlTagMock("tag1").addAttribute(XmlAttributeMock("id", "tag2"))
        val componentMock = mockk<TapestryComponent>(relaxed = true)

        val injectedElement = InjectedElement(fieldMock, componentMock)
        injectedElement.getParameters().size shouldBe 1

        val injectedElement2 = InjectedElement(tagMock, componentMock)
        injectedElement2.getParameters().size shouldBe 1
    }

    "compareTo" {
        val fieldMock = JavaFieldMock("field1", true)
        val fieldMock2 = JavaFieldMock("field2", true)
        val componentMock = mockk<TapestryComponent>(relaxed = true)

        InjectedElement(fieldMock, componentMock).compareTo(InjectedElement(fieldMock, componentMock)) shouldBe 0

        (InjectedElement(fieldMock, componentMock).compareTo(InjectedElement(fieldMock2, componentMock)) < 0) shouldBe true
    }
})
