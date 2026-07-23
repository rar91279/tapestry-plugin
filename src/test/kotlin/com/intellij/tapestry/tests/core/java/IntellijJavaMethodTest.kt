package com.intellij.tapestry.tests.core.java

import com.intellij.openapi.application.runReadAction
import com.intellij.tapestry.core.java.IJavaArrayType
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.intellij.core.java.IntellijJavaMethod
import com.intellij.tapestry.intellij.core.java.IntellijJavaPrimitiveType
import com.intellij.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

/**
 * @author <a href="mailto:hugo.palma@logical-software.com">Hugo Palma</a>
 */
class IntellijJavaMethodTest : JavaModuleFixtureSpec({

    fun method(name: String) = IntellijJavaMethod(
        module,
        javaFacade().findClass("com.app.util.Class1", allScope())!!.findMethodsByName(name, false)[0]
    )

    "test_full_method" {
        runReadAction {
            val method = method("method1")
            method.name shouldBe "method1"
            (method.returnType as IJavaClassType).fullyQualifiedName shouldBe "com.app.util.Class1"
            method.annotations.size shouldBe 2
            method.documentation shouldBe " method1 doc."
            method.parameters.size shouldBe 3
        }
    }

    "test_empty_methods" {
        runReadAction {
            val method2 = method("method2")
            val method3 = method("method3")
            val method4 = method("method4")
            val method5 = method("method5")

            method2.name shouldBe "method2"
            (method2.returnType is IJavaArrayType) shouldBe true
            method2.annotations.size shouldBe 0
            method2.documentation.isEmpty() shouldBe true
            method2.parameters.size shouldBe 0

            method3.name shouldBe "method3"
            method3.returnType.name shouldBe "int"
            method3.annotations.size shouldBe 0
            method3.documentation.isEmpty() shouldBe true
            method3.parameters.size shouldBe 0

            // void return
            (method4.returnType is IntellijJavaPrimitiveType) shouldBe true
            method4.returnType.name shouldBe "void"

            // invalid class return
            method5.returnType shouldBe null
        }
    }

    "getAnnotation" {
        runReadAction {
            val method1 = method("method1")
            method1.getAnnotation(null) shouldBe null
            method1.getAnnotation("java.lang.SuppressWarnings").fullyQualifiedName shouldBe "java.lang.SuppressWarnings"
        }
    }

    "getContainingClass" {
        runReadAction {
            method("method1").containingClass.name shouldBe "Class1"
        }
    }

    "getPsiMethod" {
        runReadAction {
            method("method1").psiMethod.name shouldBe "method1"
        }
    }
})
