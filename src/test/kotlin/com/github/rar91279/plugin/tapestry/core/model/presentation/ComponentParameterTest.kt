package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.mocks.psiAnnotationMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiFieldMock
import com.intellij.psi.PsiField
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

private const val PARAMETER = "org.apache.tapestry5.annotations.Parameter"

class ComponentParameterTest : FreeSpec({

    fun field(
        name: String = "field1",
        javadoc: String? = null,
        vararg attributes: Pair<String, List<String>>,
    ): PsiField = psiFieldMock(name, javadoc = javadoc, annotations = listOf(psiAnnotationMock(PARAMETER, *attributes)))

    "getName" {
        TapestryParameter(null, field("_field1")).name shouldBe "field1"

        TapestryParameter(null, field("\$field1")).name shouldBe "field1"

        TapestryParameter(null, field("field1")).name shouldBe "field1"

        TapestryParameter(null, field("field1", attributes = arrayOf("name" to listOf("field2")))).name shouldBe "field2"
    }

    "getDescription" {
        TapestryParameter(null, field(javadoc = "docs")).description shouldBe "docs"
    }

    "isRequired" {
        TapestryParameter(null, field()).isRequired shouldBe false
    }

    "getDefaultPrefix_default" {
        TapestryParameter(null, field()).defaultPrefix shouldBe "prop"
    }

    "getDefaultPrefix_configured" {
        TapestryParameter(null, field(attributes = arrayOf("defaultPrefix" to listOf("myprefix")))).defaultPrefix shouldBe "myprefix"
    }

    "getDefaultValue_default" {
        TapestryParameter(null, field()).defaultValue.isEmpty() shouldBe true
    }

    "getDefaultValue_configured" {
        TapestryParameter(null, field(attributes = arrayOf("value" to listOf("myvalue")))).defaultValue shouldBe "myvalue"
    }

    "compareTo" {
        val parameter1 = TapestryParameter(null, field("name1"))
        val parameter2 = TapestryParameter(null, field("name2"))

        parameter1.compareTo(TapestryParameter(null, field("name1"))) shouldBe 0

        (parameter1.compareTo(parameter2) < 0) shouldBe true
    }
})
