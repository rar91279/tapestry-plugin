package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.exceptions.NotTapestryElementException
import com.github.rar91279.plugin.tapestry.core.mocks.psiAnnotationMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiFieldMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiFileMock
import com.github.rar91279.plugin.tapestry.core.mocks.stubFields
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.resource.IResourceFinder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class TestableParameterReceiverElement(
    library: TapestryLibrary?,
    elementClass: PsiClass,
    project: TapestryProject
) : ParameterReceiverElement(library, elementClass, project) {

    override fun allowsTemplate(): Boolean = false

    override val template: Array<PsiFile> = emptyArray()

    override val messageCatalog: Array<PsiFile> = emptyArray()
}

class PresentationLibraryElementTest : FreeSpec({

    lateinit var classInBasePackageMock: PsiClass
    lateinit var classInSomePackageMock: PsiClass
    lateinit var classInComponentsPackageNotPublicMock: PsiClass
    lateinit var classInComponentsPackageNoDefaultConstructorMock: PsiClass
    lateinit var classInRootComponentsPackageMock: PsiClass
    lateinit var classInRootMixinsPackageMock: PsiClass
    lateinit var classInSubComponentsPackageMock: PsiClass
    lateinit var classInRootPagesPackageMock: PsiClass
    lateinit var rootComponentClassMock: PsiClass
    lateinit var tapestryProjectMock: TapestryProject
    lateinit var resourceFinderMock: IResourceFinder
    lateinit var libraryMock: TapestryLibrary
    lateinit var libraryNoRootPackageMock: TapestryLibrary

    beforeTest {
        val builderClassResourceMock = psiFileMock("Builder.java", timeStamp = Long.MAX_VALUE)

        fun classMock(fqn: String, isPublic: Boolean = true, hasDefaultConstructor: Boolean = true, javadoc: String? = null) =
            psiClassMock(fqn, isPublic, hasDefaultConstructor, builderClassResourceMock, javadoc)

        classInBasePackageMock = classMock("com.app.SomeClass")

        classInComponentsPackageNotPublicMock = classMock("com.app.components.SomeClass", isPublic = false)

        classInComponentsPackageNoDefaultConstructorMock =
            classMock("com.app.components.SomeClass", hasDefaultConstructor = false)

        rootComponentClassMock = classMock("com.app.components.SomeClass")

        classInSomePackageMock = classMock("com.app.test.components.SomeClass")

        classInRootComponentsPackageMock = classMock("com.app.components.SomeClass", javadoc = "docs")

        classInRootMixinsPackageMock = classMock("com.app.mixins.SomeClass")

        classInRootPagesPackageMock = classMock("com.app.pages.SomeClass")

        classInSubComponentsPackageMock = classMock("com.app.components.folder1.SomeClass")

        resourceFinderMock = mockk(relaxed = true)
        tapestryProjectMock = mockk(relaxed = true)
        every { tapestryProjectMock.applicationRootPackage } returns "com.app"
        every { tapestryProjectMock.resourceFinder } returns resourceFinderMock
        // getParameters() always adds a builtin "mixins" parameter, whose creation resolves java.lang.String.
        every { tapestryProjectMock.findClassType("java.lang.String") } returns null

        libraryMock = mockk(relaxed = true)
        every { libraryMock.basePackage } returns "com.app"
        every { libraryMock.id } returns "application"

        libraryNoRootPackageMock = mockk(relaxed = true)
        every { libraryNoRootPackageMock.basePackage } returns null
        every { libraryNoRootPackageMock.id } returns "id"
    }

    "isValidElement_outside_base_package" {
        shouldThrow<NotTapestryElementException> {
            TestableParameterReceiverElement(libraryMock, classInBasePackageMock, tapestryProjectMock)
        }
        shouldThrow<NotTapestryElementException> {
            TestableParameterReceiverElement(libraryMock, classInSomePackageMock, tapestryProjectMock)
        }
    }

    "isValidElement_not_public" {
        shouldThrow<NotTapestryElementException> {
            TestableParameterReceiverElement(libraryMock, classInComponentsPackageNotPublicMock, tapestryProjectMock)
        }
    }

    "isValidElement_no_default_constructor" {
        shouldThrow<NotTapestryElementException> {
            TestableParameterReceiverElement(libraryMock, classInComponentsPackageNoDefaultConstructorMock, tapestryProjectMock)
        }
    }

    "isValidElement_no_app_root_package" {
        shouldThrow<NotTapestryElementException> {
            TestableParameterReceiverElement(libraryNoRootPackageMock, rootComponentClassMock, tapestryProjectMock)
        }
    }

    "isValidElement_valid" {
        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock)

        TestableParameterReceiverElement(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock)

        TestableParameterReceiverElement(libraryMock, classInRootPagesPackageMock, tapestryProjectMock)
    }

    "getElementNameFromClass" {
        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).name shouldBe "SomeClass"

        TestableParameterReceiverElement(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).name shouldBe "SomeClass"

        TestableParameterReceiverElement(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).name shouldBe "folder1/SomeClass"
    }

    "getParameters_no_parameters" {
        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 1

        // @Parameter counts regardless of visibility; an unannotated field never does.
        val publicField = psiFieldMock(
            "publicField", isPrivate = false,
            annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        )
        val privateField = psiFieldMock("privateField")

        classInSubComponentsPackageMock.stubFields(publicField, privateField)

        TestableParameterReceiverElement(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 2
    }

    "getParameters_with_parameters" {
        val privateField = psiFieldMock(
            "field1", annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        )

        classInSubComponentsPackageMock.stubFields(privateField)

        TestableParameterReceiverElement(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 2
    }

    "getElementClass" {
        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock)
            .elementClass?.qualifiedName shouldBe "com.app.components.SomeClass"
    }

    "getDocumentation" {
        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).description shouldBe "docs"
    }

    "createElementInstance_component" {
        (PresentationLibraryElement.createElementInstance(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock) is TapestryComponent) shouldBe true
    }

    "createElementInstance_page" {
        (PresentationLibraryElement.createElementInstance(libraryMock, classInRootPagesPackageMock, tapestryProjectMock) is Page) shouldBe true
    }

    "createElementInstance_mixin" {
        (PresentationLibraryElement.createElementInstance(libraryMock, classInRootMixinsPackageMock, tapestryProjectMock) is Mixin) shouldBe true
    }

    "checkAllValidResources" {
        val resource1: PsiFile = psiFileMock("web.xml")
        val resource2: PsiFile = psiFileMock("web.xml")
        val resource3: PsiFile = psiFileMock("web.xml", valid = false)
        val resource4: PsiFile = psiFileMock("web.xml", valid = false)

        PresentationLibraryElement.checkAllValidResources(arrayOf(resource1, resource2)) shouldBe true

        PresentationLibraryElement.checkAllValidResources(arrayOf(resource3, resource4)) shouldBe false

        PresentationLibraryElement.checkAllValidResources(arrayOf(resource1, resource2, resource3)) shouldBe false
    }

    "getMessageCatalog" {
        val resources1 = arrayListOf<PsiFile>(
            psiFileMock("SomeClass.properties"),
            psiFileMock("SomeClass_pt.properties")
        )
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.properties", true) } returns resources1

        val resources2 = arrayListOf<PsiFile>(
            psiFileMock("SomeClass.properties"),
            psiFileMock("SomeClass_pt.properties")
        )
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/components/folder1/SomeClass.properties", true) } returns resources2

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).messageCatalog.size shouldBe 2

        resources1.clear()

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).messageCatalog.size shouldBe 2

        resources1.clear()

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).messageCatalog.size shouldBe 0
    }
})
