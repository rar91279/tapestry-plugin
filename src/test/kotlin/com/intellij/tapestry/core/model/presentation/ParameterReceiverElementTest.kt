package com.intellij.tapestry.core.model.presentation

import com.intellij.tapestry.core.TapestryProject
import com.intellij.tapestry.core.java.IJavaTypeFinder
import com.intellij.tapestry.core.mocks.JavaAnnotationMock
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.mocks.JavaFieldMock
import com.intellij.tapestry.core.model.TapestryLibrary
import com.intellij.tapestry.core.resource.IResource
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

class ParameterReceiverElementTest : FreeSpec({

    lateinit var classInSubComponentsPackageMock: JavaClassTypeMock
    lateinit var classInRootComponentsPackageMock: JavaClassTypeMock
    lateinit var libraryMock: TapestryLibrary
    lateinit var tapestryProjectMock: TapestryProject

    beforeTest {
        val builderClassFileMock = mockk<File>(relaxed = true)
        every { builderClassFileMock.lastModified() } returns Long.MAX_VALUE

        val builderClassResourceMock = mockk<IResource>(relaxed = true)
        every { builderClassResourceMock.file } returns builderClassFileMock

        classInRootComponentsPackageMock = JavaClassTypeMock("com.app.components.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        classInSubComponentsPackageMock = JavaClassTypeMock("com.app.components.folder1.SomeClass").setPublic(true).setDefaultConstructor(true).setFile(builderClassResourceMock)

        tapestryProjectMock = mockk(relaxed = true)
        every { tapestryProjectMock.applicationRootPackage } returns "com.app"

        // getParameters() always adds a builtin "mixins" parameter, whose creation resolves java.lang.String.
        val javaTypeFinderMock = mockk<IJavaTypeFinder>(relaxed = true)
        every { javaTypeFinderMock.findType("java.lang.String", true) } returns null
        every { tapestryProjectMock.javaTypeFinder } returns javaTypeFinderMock

        libraryMock = mockk(relaxed = true)
        every { libraryMock.basePackage } returns "com.app"
        every { libraryMock.id } returns "application"
    }

    "getParameters_no_parameters" {
        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 1

        val publicField = JavaFieldMock("publicField", false).addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        val privateField = JavaFieldMock("privateField", true)

        classInSubComponentsPackageMock.addField(publicField).addField(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 1
    }

    "getParameters_with_parameters" {
        val privateField = JavaFieldMock("field1", true).addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))

        classInSubComponentsPackageMock.addField(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 2
    }

    "getRequiredParameters_no_parameters" {
        val privateField = JavaFieldMock("field1", true).addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))

        classInSubComponentsPackageMock.addField(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).requiredParameters.size shouldBe 0
    }

    "getRequiredParameters_with_parameters" {
        val privateField = JavaFieldMock("field1", true)
            .addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter").addParameter("required", "true"))

        classInSubComponentsPackageMock.addField(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).requiredParameters.size shouldBe 1
    }

    "getOptionalParameters_with_parameters" {
        val privateField = JavaFieldMock("field1", true).addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter"))

        classInSubComponentsPackageMock.addField(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).optionalParameters.size shouldBe 2
    }

    "getOptionalParameters_no_parameters" {
        val privateField = JavaFieldMock("field1", true)
            .addAnnotation(JavaAnnotationMock("org.apache.tapestry5.annotations.Parameter").addParameter("required", "true"))

        classInSubComponentsPackageMock.addField(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).optionalParameters.size shouldBe 1
    }
})
