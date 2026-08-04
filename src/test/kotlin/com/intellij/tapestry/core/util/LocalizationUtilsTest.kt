package com.intellij.tapestry.core.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class LocalizationUtilsTest : FreeSpec({

    "unlocalizeFileName" {
        LocalizationUtils.unlocalizeFileName("SomeFile.properties") shouldBe "SomeFile.properties"
        LocalizationUtils.unlocalizeFileName("SomeFile_pt.properties") shouldBe "SomeFile.properties"
        LocalizationUtils.unlocalizeFileName("SomeFile_yy.properties") shouldBe "SomeFile_yy.properties"
        LocalizationUtils.unlocalizeFileName("SomeFile_pt_PT.properties") shouldBe "SomeFile.properties"
        LocalizationUtils.unlocalizeFileName("SomeFile_yy_PT.properties") shouldBe "SomeFile_yy_PT.properties"
        LocalizationUtils.unlocalizeFileName("SomeFile") shouldBe "SomeFile"
        LocalizationUtils.unlocalizeFileName("SomeFile_pt") shouldBe "SomeFile"
        LocalizationUtils.unlocalizeFileName("SomeFile_yy") shouldBe "SomeFile_yy"
        LocalizationUtils.unlocalizeFileName("SomeFile_pt_PT") shouldBe "SomeFile"
        LocalizationUtils.unlocalizeFileName("SomeFile_yy_PT") shouldBe "SomeFile_yy_PT"
    }
})
