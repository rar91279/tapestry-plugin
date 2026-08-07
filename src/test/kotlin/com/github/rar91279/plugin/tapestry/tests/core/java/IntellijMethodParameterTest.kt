package com.github.rar91279.plugin.tapestry.tests.core.java

import com.intellij.openapi.application.readActionBlocking
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaPrimitiveType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijMethodParameter
import com.github.rar91279.plugin.tapestry.tests.core.JavaModuleFixtureSpec
import io.kotest.matchers.shouldBe

/**
 * Test suite for [IntellijMethodParameter] implementation.
 *
 * This test class verifies the behavior of the IntelliJ-based method parameter wrapper,
 * specifically testing how it handles different parameter types:
 * - Class types (fully qualified class references)
 * - Primitive types (e.g., int, boolean)
 * - Other/unsupported types
 *
 * The tests use a test fixture class `com.app.util.Class1` with a method `method1`
 * that has three parameters of different types to verify the parameter extraction
 * and type detection logic.
 *
 * @author <a href="mailto:hugo.palma@logical-software.com">Hugo Palma</a>
 */
class IntellijMethodParameterTest : JavaModuleFixtureSpec({

    /**
     * Helper function to create an [IntellijMethodParameter] wrapper for a parameter of the test method.
     *
     * Retrieves the parameter at the specified index from the `method1` method of the
     * `com.app.util.Class1` test fixture class and wraps it in an [IntellijMethodParameter].
     *
     * @param index the zero-based index of the parameter to retrieve from the method's parameter list
     * @return an [IntellijMethodParameter] wrapper for the specified parameter
     */
    fun method1Parameter(index: Int) = IntellijMethodParameter(
        module,
        javaFacade().findClass("com.app.util.Class1", allScope())!!
            .findMethodsByName("method1", false)[0].parameterList.parameters[index]
    )

    /**
     * Verifies that class type parameters are correctly identified and extracted.
     *
     * Tests that the first parameter (`param1`) of type `com.app.util.Class1` is:
     * - Named correctly as "param1"
     * - Identified as an [IJavaClassType]
     * - Has the correct fully qualified name "com.app.util.Class1"
     */
    "test_class_type" {
        readActionBlocking {
            val methodParameter = method1Parameter(0)
            methodParameter.name shouldBe "param1"
            (methodParameter.type is IJavaClassType) shouldBe true
            (methodParameter.type as IJavaClassType).fullyQualifiedName shouldBe "com.app.util.Class1"
        }
    }

    /**
     * Verifies that primitive type parameters are correctly identified and extracted.
     *
     * Tests that the second parameter (`param2`) of primitive type `int` is:
     * - Named correctly as "param2"
     * - Identified as an [IJavaPrimitiveType]
     * - Has the correct type name "int"
     */
    "test_primitive_type" {
        readActionBlocking {
            val methodParameter = method1Parameter(1)
            methodParameter.name shouldBe "param2"
            (methodParameter.type is IJavaPrimitiveType) shouldBe true
            methodParameter.type!!.name shouldBe "int"
        }
    }

    /**
     * Verifies that unsupported or other parameter types are handled correctly.
     *
     * Tests that the third parameter (`param3`) which has an unsupported or unrecognized type:
     * - Is named correctly as "param3"
     * - Has its type set to null, indicating that the type is not supported or cannot be resolved
     */
    "test_other_type" {
        readActionBlocking {
            val methodParameter = method1Parameter(2)
            methodParameter.name shouldBe "param3"
            methodParameter.type shouldBe null
        }
    }
})
