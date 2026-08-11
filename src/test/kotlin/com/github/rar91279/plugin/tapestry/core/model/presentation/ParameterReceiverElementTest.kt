package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.mocks.psiAnnotationMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiFieldMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiFileMock
import com.github.rar91279.plugin.tapestry.core.mocks.stubFields
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.intellij.psi.PsiClass
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ParameterReceiverElementTest : FreeSpec({

    lateinit var classInSubComponentsPackageMock: PsiClass
    lateinit var classInRootComponentsPackageMock: PsiClass
    lateinit var libraryMock: TapestryLibrary
    lateinit var tapestryProjectMock: TapestryProject

    beforeTest {
        val builderClassResourceMock = psiFileMock("Builder.java", timeStamp = Long.MAX_VALUE)

        classInRootComponentsPackageMock =
            psiClassMock("com.app.components.SomeClass", isPublic = true, containingFile = builderClassResourceMock)

        classInSubComponentsPackageMock =
            psiClassMock("com.app.components.folder1.SomeClass", isPublic = true, containingFile = builderClassResourceMock)

        tapestryProjectMock = mockk(relaxed = true)
        every { tapestryProjectMock.applicationRootPackage } returns "com.app"
        // getParameters() always adds a builtin "mixins" parameter, whose creation resolves java.lang.String.
        every { tapestryProjectMock.findClassType("java.lang.String") } returns null

        libraryMock = mockk(relaxed = true)
        every { libraryMock.basePackage } returns "com.app"
        every { libraryMock.id } returns "application"
    }

    "getParameters_no_parameters" {
        TapestryComponent(libraryMock, classInRootComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 1

        // @Parameter counts regardless of visibility; an unannotated field never does.
        val publicField = psiFieldMock(
            "publicField", isPrivate = false,
            annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        )
        val privateField = psiFieldMock("privateField")

        classInSubComponentsPackageMock.stubFields(publicField, privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 2
    }

    "getParameters_with_parameters" {
        val privateField = psiFieldMock(
            "field1", annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        )

        classInSubComponentsPackageMock.stubFields(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).parameters.size shouldBe 2
    }

    "getRequiredParameters_no_parameters" {
        val privateField = psiFieldMock(
            "field1", annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        )

        classInSubComponentsPackageMock.stubFields(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).requiredParameters.size shouldBe 0
    }

    "getRequiredParameters_with_parameters" {
        val privateField = psiFieldMock(
            "field1",
            annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Parameter", "required" to listOf("true")))
        )

        classInSubComponentsPackageMock.stubFields(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).requiredParameters.size shouldBe 1
    }

    "getOptionalParameters_with_parameters" {
        val privateField = psiFieldMock(
            "field1", annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Parameter"))
        )

        classInSubComponentsPackageMock.stubFields(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).optionalParameters.size shouldBe 2
    }

    "getOptionalParameters_no_parameters" {
        val privateField = psiFieldMock(
            "field1",
            annotations = listOf(psiAnnotationMock("org.apache.tapestry5.annotations.Parameter", "required" to listOf("true")))
        )

        classInSubComponentsPackageMock.stubFields(privateField)

        TapestryComponent(libraryMock, classInSubComponentsPackageMock, tapestryProjectMock).optionalParameters.size shouldBe 1
    }
})
