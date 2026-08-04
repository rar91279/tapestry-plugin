package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class AbstractValueResolverTest : FreeSpec({

    "getPrefix" - {
        "null" {
            AbstractValueResolver.getPrefix(null, null) shouldBe null
        }
        "defined prefix" {
            AbstractValueResolver.getPrefix("prefix:", "default") shouldBe "prefix"
            AbstractValueResolver.getPrefix("prefix:value", "default") shouldBe "prefix"
        }
        "no defined prefix" {
            AbstractValueResolver.getPrefix(":", "default") shouldBe null
            AbstractValueResolver.getPrefix(":value", "default") shouldBe null
            AbstractValueResolver.getPrefix("value", "default") shouldBe "default"
        }
    }

    "getCleanValue" - {
        "defined prefix" {
            AbstractValueResolver.getCleanValue("prefix:") shouldBe ""
            AbstractValueResolver.getCleanValue("prefix: value ") shouldBe "value"
            AbstractValueResolver.getCleanValue("\${prefix: value }") shouldBe "value"
            AbstractValueResolver.getCleanValue("\${prefix: value ") shouldBe "value"
            AbstractValueResolver.getPrefix("\${prefix: value }", "default") shouldBe "prefix"
        }
        "no defined prefix" {
            AbstractValueResolver.getCleanValue(":") shouldBe null
            AbstractValueResolver.getCleanValue(": value ") shouldBe null
            AbstractValueResolver.getCleanValue(" value ") shouldBe "value"
            AbstractValueResolver.getCleanValue("\${ value }") shouldBe "value"
            AbstractValueResolver.getCleanValue("\${ value ") shouldBe "value"
        }
    }
})
