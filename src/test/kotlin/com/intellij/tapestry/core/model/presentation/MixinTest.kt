package com.intellij.tapestry.core.model.presentation

import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.model.TapestryLibrary
import com.intellij.tapestry.core.resource.IResourceFinder
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class MixinTest : FreeSpec({

    lateinit var classInRootPagesPackageMock: JavaClassTypeMock
    lateinit var tapestryProjectMock: TapestryProject
    lateinit var libraryMock: TapestryLibrary

    beforeTest {
        classInRootPagesPackageMock = JavaClassTypeMock("com.app.pages.SomeClass").setPublic(true).setDefaultConstructor(true)

        val resourceFinderMock = mockk<IResourceFinder>(relaxed = true)
        tapestryProjectMock = mockk(relaxed = true)
        every { tapestryProjectMock.applicationRootPackage } returns "com.app"
        every { tapestryProjectMock.resourceFinder } returns resourceFinderMock

        libraryMock = mockk(relaxed = true)
        every { libraryMock.basePackage } returns "com.app"
        every { libraryMock.id } returns "application"
    }

    "allowsTemplate" {
        Mixin(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).allowsTemplate() shouldBe false
    }

    "getTemplate" {
        Mixin(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).template.size shouldBe 0
    }

    "getMessageCatalog" {
        Mixin(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).messageCatalog.size shouldBe 0
    }
})
