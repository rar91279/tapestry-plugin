package com.github.rar91279.plugin.tapestry.tests.core

import com.github.rar91279.plugin.tapestry.core.util.attributeValues
import com.github.rar91279.plugin.tapestry.intellij.util.JavaTypeCreator
import com.intellij.openapi.application.readActionBlocking
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiModifier
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

/**
 * Test suite for [JavaTypeCreator]: field creation, annotation creation and import management.
 *
 * Tests are conditionally enabled based on JDK availability and execute within read actions to comply
 * with IntelliJ platform threading requirements.
 */
class JavaTypeCreatorTest : JavaModuleFixtureSpec({

    fun classNamed(fullyQualifiedName: String): PsiClass = javaFacade().findClass(fullyQualifiedName, allScope())!!

    fun stringType() = classNamed("java.lang.String")

    /**
     * Validates that [JavaTypeCreator.createField] correctly:
     * - Creates fields with the specified name and type
     * - Applies private visibility when requested
     * - Converts field names to camelCase when the IDE naming settings are applied
     * - Preserves original name capitalization when they are not
     */
    "createField".config(enabled = jdkAvailable) {
        readActionBlocking {
            val creator = JavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)!!
            psiField1.name shouldBe "field1"
            (psiField1.type as PsiClassType).resolve()!!.qualifiedName shouldBe "java.lang.String"
            psiField1.hasModifierProperty(PsiModifier.PRIVATE) shouldBe true

            val psiField2 = creator.createField("Field1", stringType(), true, true)!!
            psiField2.name shouldBe "field1"

            val psiField3 = creator.createField("Field1", stringType(), true, false)!!
            psiField3.name shouldBe "Field1"

            val psiField4 = creator.createField("field1", stringType(), false, true)!!
            psiField4.hasModifierProperty(PsiModifier.PRIVATE) shouldBe false
        }
    }

    "createFieldAnnotation_no_parameters".config(enabled = jdkAvailable) {
        readActionBlocking {
            val creator = JavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)!!
            creator.createFieldAnnotation(psiField1, "java.lang.Deprecated", HashMap())

            psiField1.annotations.size shouldBe 1
            psiField1.annotations.first().qualifiedName shouldBe "java.lang.Deprecated"
            psiField1.annotations.first().parameterList.attributes.size shouldBe 0
        }
    }

    "createFieldAnnotation_with_parameters".config(enabled = jdkAvailable) {
        readActionBlocking {
            val creator = JavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)!!

            val parameters = hashMapOf("param1" to "param1value", "param2" to "param2value")

            creator.createFieldAnnotation(psiField1, "java.lang.Deprecated", parameters)

            psiField1.annotations.size shouldBe 1

            val annotation = psiField1.annotations.first()

            annotation.qualifiedName shouldBe "java.lang.Deprecated"
            annotation.parameterList.attributes.size shouldBe 2
            annotation.attributeValues("param1")[0] shouldBe "param1value"
            annotation.attributeValues("param2")[0] shouldBe "param2value"
        }
    }

    /** Already-imported classes are reported as imported, without a second import statement. */
    "ensureClassImport_already_imported".config(enabled = jdkAvailable) {
        readActionBlocking {
            val creator = JavaTypeCreator(module)

            creator.ensureClassImport(
                classNamed("com.app.ModuleBuilder"), classNamed("java.util.Collection")
            ) shouldBe true
        }
    }

    /** A class that is not yet imported goes through [JavaTypeCreator.addImport]. */
    "ensureClassImport_not_imported".config(enabled = jdkAvailable) {
        readActionBlocking {
            val controlMock = mockk<JavaTypeCreator>(relaxed = true)
            val creator: JavaTypeCreator = JavaTypeCreatorDummy(module, controlMock)

            creator.ensureClassImport(
                classNamed("com.app.ModuleBuilder"), classNamed(CommonClassNames.JAVA_UTIL_MAP)
            ) shouldBe true

            verify { controlMock.addImport(any<PsiImportList>(), any<PsiClass>()) }
        }
    }
})
