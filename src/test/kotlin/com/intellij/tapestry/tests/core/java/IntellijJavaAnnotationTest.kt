package com.intellij.tapestry.tests.core.java

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.tapestry.intellij.core.java.IntellijJavaAnnotation
import com.intellij.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

/**
 * @author <a href="mailto:hugo.palma@logical-software.com">Hugo Palma</a>
 */
class IntellijJavaAnnotationTest : JavaModuleFixtureSpec({

    fun class1() = javaFacade().findClass("com.app.util.Class1", GlobalSearchScope.moduleRuntimeScope(module, false))!!

    "getFullyQualifiedName".config(enabled = jdkAvailable) {
        runReadAction {
            val annotation = IntellijJavaAnnotation(class1().modifierList!!.annotations[0])
            annotation.fullyQualifiedName shouldBe "java.lang.Deprecated"
        }
    }

    "getParameters_no_parameters" {
        runReadAction {
            val annotation = IntellijJavaAnnotation(class1().modifierList!!.annotations[0])
            annotation.parameters.size shouldBe 0
        }
    }

    "getParameters_with_parameters" {
        runReadAction {
            val annotation = IntellijJavaAnnotation(class1().modifierList!!.annotations[1])
            val parameters = annotation.parameters
            parameters.size shouldBe 1
            parameters[null]!![0] shouldBe "warning1"
        }
    }

    "getParameters_with_array_parameters" {
        runReadAction {
            val annotation = IntellijJavaAnnotation(class1().fields[0].modifierList!!.annotations[0])
            val parameters = annotation.parameters
            parameters.size shouldBe 1
            parameters["parameters"]!!.size shouldBe 3
        }
    }

    "getPsiAnnotation".config(enabled = jdkAvailable) {
        runReadAction {
            val annotation = IntellijJavaAnnotation(class1().modifierList!!.annotations[0])
            annotation.psiAnnotation.qualifiedName shouldBe "java.lang.Deprecated"
        }
    }
})
