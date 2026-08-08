package com.github.rar91279.plugin.tapestry.core.model.ioc

import com.github.rar91279.plugin.tapestry.core.mocks.psiAnnotationMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassTypeMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiFileMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiMethodMock
import com.github.rar91279.plugin.tapestry.core.mocks.stubMethods
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every

class ModuleBuilderTest : FreeSpec({

    lateinit var builderClassWithBuildMethods: PsiClass
    lateinit var returnType: PsiClass
    lateinit var buildMethod: PsiMethod
    lateinit var buildMethodWithSuffix: PsiMethod

    beforeTest {
        val builderClassResourceMock = psiFileMock("Builder.java", timeStamp = Long.MAX_VALUE)

        builderClassWithBuildMethods = psiClassMock("com.app.services.Builder", containingFile = builderClassResourceMock)

        returnType = psiClassMock("MyService")

        buildMethod = psiMethodMock("build", returnType = psiClassTypeMock(returnType))
        buildMethodWithSuffix = psiMethodMock("buildSomeService", returnType = psiClassTypeMock(returnType))
    }

    "getServices_default_service_build_no_suffix" {
        builderClassWithBuildMethods.stubMethods(buildMethod)

        val services = ModuleBuilder(builderClassWithBuildMethods).services

        services.size shouldBe 1
        services.first().id shouldBe returnType.name
    }

    "getServices_default_service_build_no_suffix_and_annotations" {
        val scopeAnnotationMock = psiAnnotationMock("org.apache.tapestry5.ioc.annotations.Scope", "value" to listOf("myscope"))
        val eagerLoadAnnotationMock = psiAnnotationMock("org.apache.tapestry5.ioc.annotations.EagerLoad")

        every { buildMethod.getAnnotation("org.apache.tapestry5.ioc.annotations.Scope") } returns scopeAnnotationMock
        every { buildMethod.getAnnotation("org.apache.tapestry5.ioc.annotations.EagerLoad") } returns eagerLoadAnnotationMock

        builderClassWithBuildMethods.stubMethods(buildMethod)

        val services = ModuleBuilder(builderClassWithBuildMethods).services

        services.size shouldBe 1
        services.first().id shouldBe returnType.name
        services.first().scope shouldBe "myscope"
        services.first().isEagerLoad shouldBe true
    }

    "getServices_default_service_build_with_suffix" {
        builderClassWithBuildMethods.stubMethods(buildMethodWithSuffix)

        val services = ModuleBuilder(builderClassWithBuildMethods).services

        services.size shouldBe 1
        services.first().id shouldBe "SomeService"
    }

    "getServices_default_service_build_with_suffix_and_annotations" {
        val scopeAnnotationMock = psiAnnotationMock("org.apache.tapestry5.ioc.annotations.Scope", "value" to listOf("myscope"))
        val eagerLoadAnnotationMock = psiAnnotationMock("org.apache.tapestry5.ioc.annotations.EagerLoad")

        every { buildMethodWithSuffix.getAnnotation("org.apache.tapestry5.ioc.annotations.Scope") } returns scopeAnnotationMock
        every { buildMethodWithSuffix.getAnnotation("org.apache.tapestry5.ioc.annotations.EagerLoad") } returns eagerLoadAnnotationMock

        builderClassWithBuildMethods.stubMethods(buildMethodWithSuffix)

        val services = ModuleBuilder(builderClassWithBuildMethods).services

        services.size shouldBe 1
        services.first().id shouldBe "SomeService"
        services.first().scope shouldBe "myscope"
        services.first().isEagerLoad shouldBe true
    }
})
