package com.intellij.tapestry.core.model.presentation

import com.intellij.tapestry.core.mocks.JavaAnnotationMock
import com.intellij.tapestry.core.mocks.JavaFieldMock
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ComponentParameterTest : FreeSpec({

    fun newField(): JavaFieldMock {
        val field = JavaFieldMock().setPrivate(true)
        field.addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        return field
    }

    lateinit var fieldMock: JavaFieldMock

    beforeTest {
        fieldMock = newField()
    }

    "getName" {
        fieldMock.setName("_field1")
        TapestryParameter(null, fieldMock).getName() shouldBe "field1"

        fieldMock = newField()
        fieldMock.setName("\$field1")
        TapestryParameter(null, fieldMock).getName() shouldBe "field1"

        fieldMock = newField()
        fieldMock.setName("field1")
        TapestryParameter(null, fieldMock).getName() shouldBe "field1"

        fieldMock = newField()
        fieldMock.setName("field1")
        (fieldMock.getAnnotations().values.first() as JavaAnnotationMock).addParameter("name", "field2")
        TapestryParameter(null, fieldMock).getName() shouldBe "field2"
    }

    "getDescription" {
        fieldMock.setDocumentation("docs")

        TapestryParameter(null, fieldMock).getDescription() shouldBe "docs"
    }

    "isRequired" {
        TapestryParameter(null, fieldMock).isRequired() shouldBe false
    }

    "getDefaultPrefix_default" {
        TapestryParameter(null, fieldMock).getDefaultPrefix() shouldBe "prop"
    }

    "getDefaultPrefix_configured" {
        (fieldMock.getAnnotations().values.first() as JavaAnnotationMock).addParameter("defaultPrefix", "myprefix")

        TapestryParameter(null, fieldMock).getDefaultPrefix() shouldBe "myprefix"
    }

    "getDefaultValue_default" {
        TapestryParameter(null, fieldMock).getDefaultValue().isEmpty() shouldBe true
    }

    "getDefaultValue_configured" {
        (fieldMock.getAnnotations().values.first() as JavaAnnotationMock).addParameter("value", "myvalue")

        TapestryParameter(null, fieldMock).getDefaultValue() shouldBe "myvalue"
    }

    "compareTo" {
        fieldMock.setName("name1")

        val field2Mock = JavaFieldMock("name2", true)
        field2Mock.addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))

        TapestryParameter(null, fieldMock).compareTo(TapestryParameter(null, fieldMock)) shouldBe 0

        (TapestryParameter(null, fieldMock).compareTo(TapestryParameter(null, field2Mock)) < 0) shouldBe true
    }
})
