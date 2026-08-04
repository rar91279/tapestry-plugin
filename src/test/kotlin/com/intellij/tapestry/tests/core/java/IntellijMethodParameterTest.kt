package com.intellij.tapestry.tests.core.java

import com.intellij.openapi.application.runReadAction
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.java.IJavaPrimitiveType
import com.intellij.tapestry.intellij.core.java.IntellijMethodParameter
import com.intellij.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

/**
 * @author <a href="mailto:hugo.palma@logical-software.com">Hugo Palma</a>
 */
class IntellijMethodParameterTest : JavaModuleFixtureSpec({

    fun method1Parameter(index: Int) = IntellijMethodParameter(
        module,
        javaFacade().findClass("com.app.util.Class1", allScope())!!
            .findMethodsByName("method1", false)[0].parameterList.parameters[index]
    )

    "test_class_type" {
        runReadAction {
            val methodParameter = method1Parameter(0)
            methodParameter.name shouldBe "param1"
            (methodParameter.type is IJavaClassType) shouldBe true
            (methodParameter.type as IJavaClassType).fullyQualifiedName shouldBe "com.app.util.Class1"
        }
    }

    "test_primitive_type" {
        runReadAction {
            val methodParameter = method1Parameter(1)
            methodParameter.name shouldBe "param2"
            (methodParameter.type is IJavaPrimitiveType) shouldBe true
            methodParameter.type!!.name shouldBe "int"
        }
    }

    "test_other_type" {
        runReadAction {
            val methodParameter = method1Parameter(2)
            methodParameter.name shouldBe "param3"
            methodParameter.type shouldBe null
        }
    }
})
