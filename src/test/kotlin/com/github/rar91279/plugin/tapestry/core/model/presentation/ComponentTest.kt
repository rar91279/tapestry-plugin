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

class ComponentTest : FreeSpec({

    lateinit var classInRootComponentsPackageMock: PsiClass
    lateinit var tapestryProjectMock: TapestryProject
    lateinit var resourceFinderMock: IResourceFinder
    lateinit var libraryMock: TapestryLibrary

    beforeTest {
        val builderClassResourceMock = psiFileMock("Builder.java", timeStamp = Long.MAX_VALUE)

        classInRootComponentsPackageMock = psiClassMock("com.app.components.SomeClass", isPublic = true, containingFile = builderClassResourceMock)

        resourceFinderMock = mockk(relaxed = true)
        tapestryProjectMock = mockk(relaxed = true)
        every { tapestryProjectMock.applicationRootPackage } returns "com.app"
        every { tapestryProjectMock.resourceFinder } returns resourceFinderMock

        libraryMock = mockk(relaxed = true)
        every { libraryMock.basePackage } returns "com.app"
        every { libraryMock.id } returns "application"
    }

    "getTemplate_no_template" {
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/components/SomeClass.tml", true) } returns emptyList()
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns emptyList()

        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).template.size shouldBe 0

        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).template.size shouldBe 0
    }

    "getTemplate_template_in_classpath" {
        val web1: Collection<PsiFile> = listOf(psiFileMock("SomeClass.tml"))
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/components/SomeClass.tml", true) } returns web1

        val templates: Collection<PsiFile> = listOf(psiFileMock("SomeClass.tml"))
        every { resourceFinderMock.findLocalizedContextResource("SomeClass.tml") } returns templates

        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).template[0].name shouldBe "SomeClass.tml"
    }

    "allowsTemplate" {
        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).allowsTemplate() shouldBe true
    }
})
