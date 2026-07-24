package com.intellij.tapestry.tests.core.java

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiImportList
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.intellij.core.java.IntellijJavaClassType
import com.intellij.tapestry.intellij.core.java.IntellijJavaTypeCreator
import com.intellij.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

class IntellijJavaTypeCreatorTest : JavaModuleFixtureSpec({

    fun stringType() =
        IntellijJavaClassType(module, javaFacade().findClass("java.lang.String", allScope())!!.containingFile)

    "createField".config(enabled = jdkAvailable) {
        runReadAction {
            val creator = IntellijJavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)
            psiField1.name shouldBe "field1"
            (psiField1.type as IJavaClassType).fullyQualifiedName shouldBe "java.lang.String"
            psiField1.isPrivate shouldBe true

            val psiField2 = creator.createField("Field1", stringType(), true, true)
            psiField2.name shouldBe "field1"

            val psiField3 = creator.createField("Field1", stringType(), true, false)
            psiField3.name shouldBe "Field1"

            val psiField4 = creator.createField("field1", stringType(), false, true)
            psiField4.isPrivate shouldBe false
        }
    }

    "createFieldAnnotation_no_parameters".config(enabled = jdkAvailable) {
        runReadAction {
            val creator = IntellijJavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)
            creator.createFieldAnnotation(psiField1, "java.lang.Deprecated", HashMap())

            psiField1.annotations.size shouldBe 1
            psiField1.annotations.values.first().fullyQualifiedName shouldBe "java.lang.Deprecated"
            psiField1.annotations.values.first().parameters.size shouldBe 0
        }
    }

    "createFieldAnnotation_with_parameters".config(enabled = jdkAvailable) {
        runReadAction {
            val creator = IntellijJavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)

            val parameters = hashMapOf("param1" to "param1value", "param2" to "param2value")

            creator.createFieldAnnotation(psiField1, "java.lang.Deprecated", parameters)

            psiField1.annotations.size shouldBe 1

            val annotation = psiField1.annotations.values.first()

            annotation.fullyQualifiedName shouldBe "java.lang.Deprecated"
            annotation.parameters.size shouldBe 2
            annotation.parameters["param1"]!![0] shouldBe "param1value"
            annotation.parameters["param2"]!![0] shouldBe "param2value"
        }
    }

    "ensureClassImport_already_imported".config(enabled = jdkAvailable) {
        runReadAction {
            val testedClass = IntellijJavaClassType(
                module, javaFacade().findClass("com.app.ModuleBuilder", allScope())!!.containingFile
            )
            val creator = IntellijJavaTypeCreator(module)

            creator.ensureClassImport(
                testedClass,
                IntellijJavaClassType(module, javaFacade().findClass("java.util.Collection", allScope())!!.containingFile)
            ) shouldBe true
        }
    }

    "ensureClassImport_not_imported".config(enabled = jdkAvailable) {
        runReadAction {
            val controlMock = mockk<IntellijJavaTypeCreator>(relaxed = true)

            val testedClass = IntellijJavaClassType(
                module, javaFacade().findClass("com.app.ModuleBuilder", allScope())!!.containingFile
            )
            val creator: IntellijJavaTypeCreator = IntellijJavaTypeCreatorDummy(module, controlMock)

            creator.ensureClassImport(
                testedClass,
                IntellijJavaClassType(module, javaFacade().findClass(CommonClassNames.JAVA_UTIL_MAP, allScope())!!.containingFile)
            ) shouldBe true

            verify { controlMock.addImport(any<PsiImportList>(), any<PsiClass>()) }
        }
    }
})
