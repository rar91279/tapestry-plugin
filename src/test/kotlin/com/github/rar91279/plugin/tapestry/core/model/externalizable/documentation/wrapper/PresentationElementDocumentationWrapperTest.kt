package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.wrapper

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class PresentationElementDocumentationWrapperTest : FreeSpec({

    "complete" {
        val wrapper = PresentationElementDocumentationWrapper(
            javaClass.getResource("/documentation/presentation/Complete.xml")
        )

        // check description
        wrapper.description shouldBe "Component that triggers an action on the server with a subsequent full page refresh."

        // check parameters
        wrapper.getParameterDescription("context") shouldBe "value1"
        wrapper.getParameterDescription("disabled") shouldBe "value2"
        wrapper.getParameterDescription("dontexist") shouldBe ""

        // check examples
        wrapper.examples shouldBe "Some component examples in HTML."

        // check notes
        wrapper.notes shouldBe "Some component notes in HTML."
    }

    "empty" {
        val wrapper = PresentationElementDocumentationWrapper(
            javaClass.getResource("/documentation/presentation/Empty.xml")
        )

        // check description
        wrapper.description.length shouldBe 0

        // check examples
        wrapper.examples.length shouldBe 0

        // check notes
        wrapper.notes.length shouldBe 0
    }

    "no_resource" {
        val wrapper = PresentationElementDocumentationWrapper()

        // check description
        wrapper.description.length shouldBe 0

        // check examples
        wrapper.examples.length shouldBe 0

        // check notes
        wrapper.notes.length shouldBe 0
    }
})
