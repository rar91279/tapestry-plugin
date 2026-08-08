package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.intellij.psi.PsiClass
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.intellij.psi.PsiFile
import com.github.rar91279.plugin.tapestry.core.resource.IResourceFinder
import com.github.rar91279.plugin.tapestry.core.mocks.psiFileMock
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class PageTest : FreeSpec({

    lateinit var classInRootPagesPackageMock: PsiClass
    lateinit var tapestryProjectMock: TapestryProject
    lateinit var resourceFinderMock: IResourceFinder
    lateinit var libraryMock: TapestryLibrary

    beforeTest {
        classInRootPagesPackageMock = psiClassMock("com.app.pages.SomeClass", isPublic = true)

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
        val web1: Collection<PsiFile> = listOf(psiFileMock("SomeClass.tml"))
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.tml", true) } returns web1
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns emptyList()

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).template[0].name shouldBe "SomeClass.tml"
    }

    "getTemplate_template_in_context" {
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.tml", true) } returns emptyList()

        val templates: Collection<PsiFile> = listOf(psiFileMock("SomeClass.tml"))
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns templates

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).template[0].name shouldBe "SomeClass.tml"
    }

    "getTemplate_template_in_both" {
        val web1: Collection<PsiFile> = listOf(psiFileMock("SomeClass.tml"))
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.tml", true) } returns web1
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns web1

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).template.size shouldBe 2
    }

    "allowsTemplate" {
        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).allowsTemplate() shouldBe true
    }
})
