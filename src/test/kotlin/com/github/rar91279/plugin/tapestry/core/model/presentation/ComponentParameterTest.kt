package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.mocks.JavaAnnotationMock
import com.github.rar91279.plugin.tapestry.core.mocks.JavaFieldMock
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
        TapestryParameter(null, fieldMock).name shouldBe "field1"

        fieldMock = newField()
        fieldMock.setName("\$field1")
        TapestryParameter(null, fieldMock).name shouldBe "field1"

        fieldMock = newField()
        fieldMock.setName("field1")
        TapestryParameter(null, fieldMock).name shouldBe "field1"

        fieldMock = newField()
        fieldMock.setName("field1")
        (fieldMock.annotations.values.first() as JavaAnnotationMock).addParameter("name", "field2")
        TapestryParameter(null, fieldMock).name shouldBe "field2"
    }

    "getDescription" {
        fieldMock.setDocumentation("docs")

        TapestryParameter(null, fieldMock).description shouldBe "docs"
    }

    "isRequired" {
        TapestryParameter(null, fieldMock).isRequired shouldBe false
    }

    "getDefaultPrefix_default" {
        TapestryParameter(null, fieldMock).defaultPrefix shouldBe "prop"
    }

    "getDefaultPrefix_configured" {
        (fieldMock.annotations.values.first() as JavaAnnotationMock).addParameter("defaultPrefix", "myprefix")

        TapestryParameter(null, fieldMock).defaultPrefix shouldBe "myprefix"
    }

    "getDefaultValue_default" {
        TapestryParameter(null, fieldMock).defaultValue.isEmpty() shouldBe true
    }

    "getDefaultValue_configured" {
        (fieldMock.annotations.values.first() as JavaAnnotationMock).addParameter("value", "myvalue")

        TapestryParameter(null, fieldMock).defaultValue shouldBe "myvalue"
    }

    "compareTo" {
        fieldMock.setName("name1")

        val field2Mock = JavaFieldMock("name2", true)
        field2Mock.addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))

        TapestryParameter(null, fieldMock).compareTo(TapestryParameter(null, fieldMock)) shouldBe 0

        (TapestryParameter(null, fieldMock).compareTo(TapestryParameter(null, field2Mock)) < 0) shouldBe true
    }
})
