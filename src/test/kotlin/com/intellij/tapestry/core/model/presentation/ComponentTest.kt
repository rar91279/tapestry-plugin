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
import java.io.File

class ComponentTest : FreeSpec({

    lateinit var classInRootComponentsPackageMock: JavaClassTypeMock
    lateinit var tapestryProjectMock: TapestryProject
    lateinit var resourceFinderMock: IResourceFinder
    lateinit var libraryMock: TapestryLibrary

    beforeTest {
        val builderClassFileMock = mockk<File>(relaxed = true)
        every { builderClassFileMock.lastModified() } returns Long.MAX_VALUE

        val builderClassResourceMock = mockk<IResource>(relaxed = true)
        every { builderClassResourceMock.getFile() } returns builderClassFileMock

        classInRootComponentsPackageMock = JavaClassTypeMock("com.app.components.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        resourceFinderMock = mockk(relaxed = true)
        tapestryProjectMock = mockk(relaxed = true)
        every { tapestryProjectMock.getApplicationRootPackage() } returns "com.app"
        every { tapestryProjectMock.getResourceFinder() } returns resourceFinderMock

        libraryMock = mockk(relaxed = true)
        every { libraryMock.getBasePackage() } returns "com.app"
        every { libraryMock.getId() } returns "application"
    }

    "getTemplate_no_template" {
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/components/SomeClass.tml", true) } returns emptyList()
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns emptyList()

        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).getTemplate().size shouldBe 0

        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).getTemplate().size shouldBe 0
    }

    "getTemplate_template_in_classpath" {
        val web1: Collection<IResource> = listOf(TestableResource("SomeClass.tml", "web1.xml"))
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/components/SomeClass.tml", true) } returns web1

        val templates: Collection<IResource> = listOf(TestableResource("SomeClass.tml", "web2.xml"))
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns templates

        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).getTemplate()[0].getName() shouldBe "SomeClass.tml"
    }

    "allowsTemplate" {
        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).allowsTemplate() shouldBe true
    }
})
