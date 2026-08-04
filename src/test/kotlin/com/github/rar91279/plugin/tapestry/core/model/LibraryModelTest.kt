package com.github.rar91279.plugin.tapestry.core.model

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaTypeFinder
import com.github.rar91279.plugin.tapestry.core.mocks.JavaClassTypeMock
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class LibraryModelTest : FreeSpec({

    "constructor" {
        val library = TapestryLibrary("id", "basepackage", null)

        library.id shouldBe "id"
        library.basePackage shouldBe "basepackage"
    }

    "compareTo" {
        val library1 = TapestryLibrary("application", "1", null)
        val library11 = TapestryLibrary("application", "1", null)
        val library2 = TapestryLibrary("application", "2", null)

        library1.compareTo(library11) shouldBe 0
        (library11.compareTo(library2) < 0) shouldBe true
    }

    "getComponents" {
        val javaTypeFinderMock = mockk<IJavaTypeFinder>(relaxed = true)
        every { javaTypeFinderMock.findTypesInPackageRecursively("com.app.components", true) } returns
            arrayListOf<IJavaClassType>(JavaClassTypeMock("com.app.components.Component1").setPublic(true).setDefaultConstructor(true))

        val tapestryProjectMock = mockk<TapestryProject>(relaxed = true)
        every { tapestryProjectMock.javaTypeFinder } returns javaTypeFinderMock

        val library = TapestryLibrary("application", "com.app", tapestryProjectMock)
        library.components.size shouldBe 1
    }

    "getPages" {
        val javaTypeFinderMock = mockk<IJavaTypeFinder>(relaxed = true)
        every { javaTypeFinderMock.findTypesInPackageRecursively("com.app.pages", true) } returns
            arrayListOf<IJavaClassType>(JavaClassTypeMock("com.app.pages.Page1").setPublic(true).setDefaultConstructor(true))

        val tapestryProjectMock = mockk<TapestryProject>(relaxed = true)
        every { tapestryProjectMock.javaTypeFinder } returns javaTypeFinderMock

        val library = TapestryLibrary("application", "com.app", tapestryProjectMock)
        library.pages.size shouldBe 1
    }

    "getMixins" {
        val javaTypeFinderMock = mockk<IJavaTypeFinder>(relaxed = true)
        every { javaTypeFinderMock.findTypesInPackageRecursively("com.app.mixins", true) } returns
            arrayListOf<IJavaClassType>(JavaClassTypeMock("com.app.mixins.Mixin1").setPublic(true).setDefaultConstructor(true))

        val tapestryProjectMock = mockk<TapestryProject>(relaxed = true)
        every { tapestryProjectMock.javaTypeFinder } returns javaTypeFinderMock

        val library = TapestryLibrary("application", "com.app", tapestryProjectMock)
        library.mixins.size shouldBe 1
    }

    "equals" {
        val library1 = TapestryLibrary(null, "com.app1", null)
        val library2 = TapestryLibrary(null, "com.app2", null)
        val library3 = TapestryLibrary(null, "com.app1", null)

        library1.equals(null) shouldBe false
        library3.equals("hey") shouldBe false
        library1.equals(library2) shouldBe false
        library1.equals(library3) shouldBe true
    }

    "hashCode_test" {
        val library1 = TapestryLibrary(null, "com.app1", null)

        library1.hashCode() shouldBe "com.app1".hashCode()
    }
})
