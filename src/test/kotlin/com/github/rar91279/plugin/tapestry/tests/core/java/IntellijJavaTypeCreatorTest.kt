package com.github.rar91279.plugin.tapestry.tests.core.java

import com.intellij.openapi.application.readActionBlocking
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiImportList
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaTypeCreator
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

/**
 * Test suite for [IntellijJavaTypeCreator] functionality.
 *
 * This test class validates the Java type creation capabilities provided by [IntellijJavaTypeCreator],
 * including field creation, annotation creation, and import management. Tests are conditionally enabled
 * based on JDK availability and execute within read actions to comply with IntelliJ platform threading requirements.
 *
 * All tests extend [JavaModuleFixtureSpec] to provide a Java module testing environment with access
 * to PSI (Program Structure Interface) elements.
 */
class IntellijJavaTypeCreatorTest : JavaModuleFixtureSpec({

    /**
     * Creates a Java type wrapper for java.lang.String.
     *
     * This helper function provides a convenient way to obtain an [IntellijJavaClassType]
     * representing the java.lang.String class for use in test assertions.
     *
     * @return an IntellijJavaClassType wrapping the java.lang.String class
     */
    fun stringType() =
        IntellijJavaClassType(module, javaFacade().findClass("java.lang.String", allScope())!!.containingFile)

    /**
     * Tests field creation with various visibility and naming options.
     *
     * Validates that [IntellijJavaTypeCreator.createField] correctly:
     * - Creates fields with the specified name and type
     * - Applies private visibility when requested
     * - Converts field names to camelCase when makeFirstLetterLowerCase is true
     * - Preserves original name capitalization when makeFirstLetterLowerCase is false
     * - Creates fields with public visibility when private is false
     */
    "createField".config(enabled = jdkAvailable) {
        readActionBlocking {
            val creator = IntellijJavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)!!
            psiField1.name shouldBe "field1"
            (psiField1.type as IJavaClassType).fullyQualifiedName shouldBe "java.lang.String"
            psiField1.isPrivate shouldBe true

            val psiField2 = creator.createField("Field1", stringType(), true, true)!!
            psiField2.name shouldBe "field1"

            val psiField3 = creator.createField("Field1", stringType(), true, false)!!
            psiField3.name shouldBe "Field1"

            val psiField4 = creator.createField("field1", stringType(), false, true)!!
            psiField4.isPrivate shouldBe false
        }
    }

    /**
     * Tests creation of field annotations without parameters.
     *
     * Validates that [IntellijJavaTypeCreator.createFieldAnnotation] correctly:
     * - Adds an annotation to a field when no parameters are provided
     * - Sets the correct fully qualified name for the annotation
     * - Creates an annotation with an empty parameter map
     */
    "createFieldAnnotation_no_parameters".config(enabled = jdkAvailable) {
        readActionBlocking {
            val creator = IntellijJavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)!!
            creator.createFieldAnnotation(psiField1, "java.lang.Deprecated", HashMap())

            psiField1.annotations.size shouldBe 1
            psiField1.annotations.values.first().fullyQualifiedName shouldBe "java.lang.Deprecated"
            psiField1.annotations.values.first().parameters.size shouldBe 0
        }
    }

    /**
     * Tests creation of field annotations with multiple parameters.
     *
     * Validates that [IntellijJavaTypeCreator.createFieldAnnotation] correctly:
     * - Adds an annotation to a field with the specified parameters
     * - Sets the correct fully qualified name for the annotation
     * - Preserves all parameter names and values in the annotation
     * - Maps parameter names to their corresponding values correctly
     */
    "createFieldAnnotation_with_parameters".config(enabled = jdkAvailable) {
        readActionBlocking {
            val creator = IntellijJavaTypeCreator(module)

            val psiField1 = creator.createField("field1", stringType(), true, true)!!

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

    /**
     * Tests import management when the class is already imported.
     *
     * Validates that [IntellijJavaTypeCreator.ensureClassImport]:
     * - Returns true when the target class is already imported in the source file
     * - Does not add duplicate imports for classes that are already present
     * - Correctly identifies existing imports in the class's import list
     */
    "ensureClassImport_already_imported".config(enabled = jdkAvailable) {
        readActionBlocking {
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

    /**
     * Tests import management when the class is not yet imported.
     *
     * Validates that [IntellijJavaTypeCreator.ensureClassImport]:
     * - Returns true when successfully adding a new import
     * - Calls the internal addImport method to add the missing import statement
     * - Correctly identifies classes that are not yet imported
     *
     * Uses a mock to verify that the addImport method is invoked with the correct parameters.
     */
    "ensureClassImport_not_imported".config(enabled = jdkAvailable) {
        readActionBlocking {
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
