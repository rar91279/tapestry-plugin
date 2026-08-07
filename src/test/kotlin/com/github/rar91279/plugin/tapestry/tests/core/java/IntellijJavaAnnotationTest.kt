package com.github.rar91279.plugin.tapestry.tests.core.java

import com.intellij.openapi.application.readActionBlocking
import com.intellij.psi.search.GlobalSearchScope
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaAnnotation
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

/**
 * Test suite for [IntellijJavaAnnotation] class.
 *
 * This test class verifies the functionality of the IntelliJ Java annotation wrapper,
 * including retrieving fully qualified names, accessing annotation parameters,
 * and handling different parameter types (single values, arrays).
 *
 * @author <a href="mailto:hugo.palma@logical-software.com">Hugo Palma</a>
 */
class IntellijJavaAnnotationTest : JavaModuleFixtureSpec({

    /**
     * Helper function to retrieve the test Java class `Class1` from the module.
     *
     * @return the PsiClass instance for `com.app.util.Class1`
     */
    fun class1() = javaFacade().findClass("com.app.util.Class1", GlobalSearchScope.moduleRuntimeScope(module, false))!!

    /**
     * Verifies that [IntellijJavaAnnotation.fullyQualifiedName] returns the correct
     * fully qualified name for an annotation.
     */
    "getFullyQualifiedName".config(enabled = jdkAvailable) {
        readActionBlocking {
            val annotation = IntellijJavaAnnotation(class1().modifierList!!.annotations[0])
            annotation.fullyQualifiedName shouldBe "java.lang.Deprecated"
        }
    }

    /**
     * Verifies that [IntellijJavaAnnotation.parameters] returns an empty map
     * for annotations without parameters.
     */
    "getParameters_no_parameters" {
        readActionBlocking {
            val annotation = IntellijJavaAnnotation(class1().modifierList!!.annotations[0])
            annotation.parameters.size shouldBe 0
        }
    }

    /**
     * Verifies that [IntellijJavaAnnotation.parameters] correctly retrieves
     * annotation parameters with single values.
     */
    "getParameters_with_parameters" {
        readActionBlocking {
            val annotation = IntellijJavaAnnotation(class1().modifierList!!.annotations[1])
            val parameters = annotation.parameters
            parameters.size shouldBe 1
            parameters[null]!![0] shouldBe "warning1"
        }
    }

    /**
     * Verifies that [IntellijJavaAnnotation.parameters] correctly retrieves
     * annotation parameters containing array values.
     */
    "getParameters_with_array_parameters" {
        readActionBlocking {
            val annotation = IntellijJavaAnnotation(class1().fields[0].modifierList!!.annotations[0])
            val parameters = annotation.parameters
            parameters.size shouldBe 1
            parameters["parameters"]!!.size shouldBe 3
        }
    }

    /**
     * Verifies that [IntellijJavaAnnotation.psiAnnotation] returns the underlying
     * PSI annotation with the correct qualified name.
     */
    "getPsiAnnotation".config(enabled = jdkAvailable) {
        readActionBlocking {
            val annotation = IntellijJavaAnnotation(class1().modifierList!!.annotations[0])
            annotation.psiAnnotation.qualifiedName shouldBe "java.lang.Deprecated"
        }
    }
})
