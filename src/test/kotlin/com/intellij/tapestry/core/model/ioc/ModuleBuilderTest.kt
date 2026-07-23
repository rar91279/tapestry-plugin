package com.intellij.tapestry.core.model.ioc

import com.intellij.tapestry.core.java.IJavaMethod
import com.intellij.tapestry.core.mocks.JavaAnnotationMock
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.resource.IResource
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

class ModuleBuilderTest : FreeSpec({

    lateinit var builderClassWithBuildMethods: JavaClassTypeMock
    lateinit var returnType: JavaClassTypeMock
    lateinit var buildMethod: IJavaMethod
    lateinit var buildMethodWithSuffix: IJavaMethod

    beforeTest {
        val builderClassFileMock = mockk<File>(relaxed = true)
        every { builderClassFileMock.lastModified() } returns Long.MAX_VALUE

        val builderClassResourceMock = mockk<IResource>(relaxed = true)
        every { builderClassResourceMock.file } returns builderClassFileMock

        builderClassWithBuildMethods = JavaClassTypeMock().setFile(builderClassResourceMock)

        returnType = JavaClassTypeMock("MyService")

        buildMethod = mockk(relaxed = true)
        every { buildMethod.name } returns "build"
        every { buildMethod.returnType } returns returnType

        buildMethodWithSuffix = mockk(relaxed = true)
        every { buildMethodWithSuffix.name } returns "buildSomeService"
        every { buildMethodWithSuffix.returnType } returns returnType
    }

    "getServices_default_service_build_no_suffix" {
        every { buildMethod.getAnnotation(match { it.startsWith("org.apache.tapestry5.ioc.annotations") }) } returns null

        builderClassWithBuildMethods.addPublicMethod(buildMethod)

        val services = ModuleBuilder(builderClassWithBuildMethods, null).services

        services.size shouldBe 1
        services.first().id shouldBe returnType.name
    }

    "getServices_default_service_build_no_suffix_and_annotations" {
        val scopeAnnotationMock = JavaAnnotationMock().addParameter("value", "myscope")
        val eagerLoadAnnotationMock = JavaAnnotationMock()

        every { buildMethod.getAnnotation(match { it.matches(Regex("org.apache.tapestry5.ioc.annotations.Scope")) }) } returns scopeAnnotationMock
        every { buildMethod.getAnnotation(match { it.matches(Regex("org.apache.tapestry5.ioc.annotations.EagerLoad")) }) } returns eagerLoadAnnotationMock

        builderClassWithBuildMethods.addPublicMethod(buildMethod)

        val services = ModuleBuilder(builderClassWithBuildMethods, null).services

        services.size shouldBe 1
        services.first().id shouldBe returnType.name
        services.first().scope shouldBe "myscope"
        services.first().isEagerLoad shouldBe true
    }

    "getServices_default_service_build_with_suffix" {
        every { buildMethodWithSuffix.getAnnotation(match { it.startsWith("org.apache.tapestry5.ioc.annotations") }) } returns null

        builderClassWithBuildMethods.addPublicMethod(buildMethodWithSuffix)

        val services = ModuleBuilder(builderClassWithBuildMethods, null).services

        services.size shouldBe 1
        services.first().id shouldBe "SomeService"
    }

    "getServices_default_service_build_with_suffix_and_annotations" {
        val scopeAnnotationMock = JavaAnnotationMock().addParameter("value", "myscope")
        val eagerLoadAnnotationMock = JavaAnnotationMock()

        every { buildMethodWithSuffix.getAnnotation(match { it.matches(Regex("org.apache.tapestry5.ioc.annotations.Scope")) }) } returns scopeAnnotationMock
        every { buildMethodWithSuffix.getAnnotation(match { it.matches(Regex("org.apache.tapestry5.ioc.annotations.EagerLoad")) }) } returns eagerLoadAnnotationMock

        builderClassWithBuildMethods.addPublicMethod(buildMethodWithSuffix)

        val services = ModuleBuilder(builderClassWithBuildMethods, null).services

        services.size shouldBe 1
        services.first().id shouldBe "SomeService"
        services.first().scope shouldBe "myscope"
        services.first().isEagerLoad shouldBe true
    }
})
