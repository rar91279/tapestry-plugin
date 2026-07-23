package com.intellij.tapestry.core.model.presentation

import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.exceptions.NotTapestryElementException
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.java.IJavaTypeFinder
import com.intellij.tapestry.core.mocks.JavaAnnotationMock
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.mocks.JavaFieldMock
import com.intellij.tapestry.core.model.TapestryLibrary
import com.intellij.tapestry.core.resource.IResource
import com.intellij.tapestry.core.resource.IResourceFinder
import com.intellij.tapestry.core.resource.TestableResource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

private class TestableParameterReceiverElement(
    library: TapestryLibrary?,
    elementClass: IJavaClassType,
    project: TapestryProject
) : ParameterReceiverElement(library, elementClass, project) {

    override fun allowsTemplate(): Boolean = false

    override fun getTemplate(): Array<IResource>? = null

    override fun getMessageCatalog(): Array<IResource> = IResource.EMPTY_ARRAY
}

class PresentationLibraryElementTest : FreeSpec({

    lateinit var classInBasePackageMock: JavaClassTypeMock
    lateinit var classInSomePackageMock: JavaClassTypeMock
    lateinit var classInComponentsPackageNotPublicMock: JavaClassTypeMock
    lateinit var classInComponentsPackageNoDefaultConstructorMock: JavaClassTypeMock
    lateinit var classInRootComponentsPackageMock: JavaClassTypeMock
    lateinit var classInRootMixinsPackageMock: JavaClassTypeMock
    lateinit var classInSubComponentsPackageMock: JavaClassTypeMock
    lateinit var classInRootPagesPackageMock: JavaClassTypeMock
    lateinit var rootComponentClassMock: JavaClassTypeMock
    lateinit var tapestryProjectMock: TapestryProject
    lateinit var resourceFinderMock: IResourceFinder
    lateinit var libraryMock: TapestryLibrary
    lateinit var libraryNoRootPackageMock: TapestryLibrary

    beforeTest {
        val builderClassFileMock = mockk<File>(relaxed = true)
        every { builderClassFileMock.lastModified() } returns Long.MAX_VALUE

        val builderClassResourceMock = mockk<IResource>(relaxed = true)
        every { builderClassResourceMock.getFile() } returns builderClassFileMock

        classInBasePackageMock = JavaClassTypeMock("com.app.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        classInComponentsPackageNotPublicMock = JavaClassTypeMock("com.app.components.SomeClass").setPublic(false).setDefaultConstructor(true).setFile(builderClassResourceMock)

        classInComponentsPackageNoDefaultConstructorMock = JavaClassTypeMock("com.app.components.SomeClass").setPublic(true).setDefaultConstructor(false).setFile(builderClassResourceMock)

        rootComponentClassMock = JavaClassTypeMock("com.app.components.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        classInSomePackageMock = JavaClassTypeMock("com.app.test.components.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        classInRootComponentsPackageMock = JavaClassTypeMock("com.app.components.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        classInRootMixinsPackageMock = JavaClassTypeMock("com.app.mixins.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        classInRootPagesPackageMock = JavaClassTypeMock("com.app.pages.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        classInSubComponentsPackageMock = JavaClassTypeMock("com.app.components.folder1.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        resourceFinderMock = mockk(relaxed = true)
        tapestryProjectMock = mockk(relaxed = true)
        every { tapestryProjectMock.getApplicationRootPackage() } returns "com.app"
        every { tapestryProjectMock.getResourceFinder() } returns resourceFinderMock

        // getParameters() always adds a builtin "mixins" parameter, whose creation resolves java.lang.String.
        val javaTypeFinderMock = mockk<IJavaTypeFinder>(relaxed = true)
        every { javaTypeFinderMock.findType("java.lang.String", true) } returns null
        every { tapestryProjectMock.getJavaTypeFinder() } returns javaTypeFinderMock

        libraryMock = mockk(relaxed = true)
        every { libraryMock.getBasePackage() } returns "com.app"
        every { libraryMock.getId() } returns "application"

        libraryNoRootPackageMock = mockk(relaxed = true)
        every { libraryNoRootPackageMock.getBasePackage() } returns null
        every { libraryNoRootPackageMock.getId() } returns "id"
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
        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).getName() shouldBe "SomeClass"

        TestableParameterReceiverElement(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).getName() shouldBe "SomeClass"

        TestableParameterReceiverElement(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).getName() shouldBe "folder1/SomeClass"
    }

    "getParameters_no_parameters" {
        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).getParameters().size shouldBe 1

        val publicField = JavaFieldMock("publicField", false).addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        val privateField = JavaFieldMock("privateField", true)

        classInSubComponentsPackageMock.addField(publicField).addField(privateField)

        TestableParameterReceiverElement(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).getParameters().size shouldBe 1
    }

    "getParameters_with_parameters" {
        val privateField = JavaFieldMock("field1", true).addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))

        classInSubComponentsPackageMock.addField(privateField)

        TestableParameterReceiverElement(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).getParameters().size shouldBe 2
    }

    "getElementClass" {
        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).getElementClass().getFullyQualifiedName() shouldBe "com.app.components.SomeClass"
    }

    "getDocumentation" {
        classInRootComponentsPackageMock.setDocumentation("docs")

        TestableParameterReceiverElement(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).getDescription() shouldBe "docs"
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
        val resource1: IResource = TestableResource("web.xml", "web1.xml")
        val resource2: IResource = TestableResource("web.xml", "web2.xml")
        val resource3: IResource = TestableResource("web.xml", "idontexist1.xml")
        val resource4: IResource = TestableResource("web.xml", "idontexist2.xml")

        PresentationLibraryElement.checkAllValidResources(arrayOf(resource1, resource2)) shouldBe true

        PresentationLibraryElement.checkAllValidResources(arrayOf(resource3, resource4)) shouldBe false

        PresentationLibraryElement.checkAllValidResources(arrayOf(resource1, resource2, resource3)) shouldBe false
    }

    "getMessageCatalog" {
        val resources1 = arrayListOf<IResource>(
            TestableResource("SomeClass.properties", "SomeClass.properties"),
            TestableResource("SomeClass_pt.properties", "SomeClass_pt.properties")
        )
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/pages/SomeClass.properties", true) } returns resources1

        val resources2 = arrayListOf<IResource>(
            TestableResource("SomeClass.properties", "SomeClass.properties"),
            TestableResource("SomeClass_pt.properties", "SomeClass_pt.properties")
        )
        every { resourceFinderMock.findLocalizedClasspathResource("com/app/components/folder1/SomeClass.properties", true) } returns resources2

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).getMessageCatalog().size shouldBe 2

        resources1.clear()

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).getMessageCatalog().size shouldBe 2

        resources1.clear()

        Page(libraryMock, classInRootPagesPackageMock, tapestryProjectMock).getMessageCatalog().size shouldBe 0
    }
})
