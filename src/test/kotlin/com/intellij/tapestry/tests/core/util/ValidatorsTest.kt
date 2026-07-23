package com.intellij.tapestry.tests.core.util

import com.intellij.tapestry.intellij.util.Validators
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ValidatorsTest : FreeSpec({

    "constructor is callable" {
        Validators()
    }

    "isValidPackageName_empty" {
        Validators.isValidPackageName(null)
        Validators.isValidPackageName("") shouldBe false
    }

    "isValidPackageName_not_valid" {
        Validators.isValidPackageName("1") shouldBe false

        Validators.isValidPackageName("1abc") shouldBe false

        Validators.isValidPackageName("a&b") shouldBe false

        Validators.isValidPackageName("a..b") shouldBe false

        Validators.isValidPackageName(".a") shouldBe false
    }

    "isValidPackageName_valid" {
        Validators.isValidPackageName("a") shouldBe true

        Validators.isValidPackageName("a.b") shouldBe true

        Validators.isValidPackageName("a1.b2") shouldBe true

        Validators.isValidPackageName("A") shouldBe true
    }

    "isValidComponentName_empty" {
        Validators.isValidComponentName(null)
        Validators.isValidComponentName("") shouldBe false
    }

    "isValidComponentName_not_valid" {
        Validators.isValidComponentName("1") shouldBe false

        Validators.isValidComponentName("a.b") shouldBe false

        Validators.isValidComponentName("a\\b") shouldBe false
    }

    "isValidComponentName_valid" {
        Validators.isValidComponentName("a") shouldBe true

        Validators.isValidComponentName("a/b") shouldBe true
    }
})
