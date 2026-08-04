package com.intellij.tapestry.core.model.presentation

import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.model.TapestryLibrary
import com.intellij.tapestry.core.resource.IResource
import com.intellij.tapestry.core.resource.IResourceFinder
import com.intellij.tapestry.core.resource.TestableResource
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class PageTest : FreeSpec({

    lateinit var classInRootPagesPackageMock: JavaClassTypeMock
    lateinit var tapestryProjectMock: TapestryProject
    lateinit var resourceFinderMock: IResourceFinder
    lateinit var libraryMock: TapestryLibrary

    beforeTest {
        classInRootPagesPackageMock = JavaClassTypeMock("com.app.pages.SomeClass").setPublic(true).setDefaultConstructor(true)

        resourceFinderMock = mockk(relaxed = true)
        tapestryProjectMock = mockk(relaxed = true)
        every { tapestryProjectMock.applicationRootPackage } returns "com.app"
        every { tapestryProjectMock.resourceFinder } returns resourceFinderMock

        libraryMock = mockk(relaxed = true)
        every { libraryMock.basePackage } returns "com.app"
        every { libraryMock.id } returns "application"
    }

    "getTemplate_no_template" {
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.tml", true) } returns emptyList()
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns emptyList()

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).template.size shouldBe 0
    }

    "getTemplate_template_in_classpath" {
        val web1: Collection<IResource> = listOf(TestableResource("SomeClass.tml", "web1.xml"))
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.tml", true) } returns web1
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns emptyList()

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).template[0].name shouldBe "SomeClass.tml"
    }

    "getTemplate_template_in_context" {
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.tml", true) } returns emptyList()

        val templates: Collection<IResource> = listOf(TestableResource("SomeClass.tml", "web2.xml"))
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns templates

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).template[0].name shouldBe "SomeClass.tml"
    }

    "getTemplate_template_in_both" {
        val web1: Collection<IResource> = listOf(TestableResource("SomeClass.tml", "web1.xml"))
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.tml", true) } returns web1
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns web1

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).template.size shouldBe 2
    }

    "allowsTemplate" {
        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).allowsTemplate() shouldBe true
    }
})
