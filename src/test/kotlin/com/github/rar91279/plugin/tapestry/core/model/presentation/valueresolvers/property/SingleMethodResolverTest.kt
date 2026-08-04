package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod
import com.github.rar91279.plugin.tapestry.core.mocks.JavaClassTypeMock
import com.github.rar91279.plugin.tapestry.core.mocks.JavaMethodMock
import com.github.rar91279.plugin.tapestry.core.mocks.MethodParameterMock
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SingleMethodResolverTest : FreeSpec({
    val resolver = SingleMethodResolver()
    lateinit var m: SpecialCaseMocks
    lateinit var contextClassType: JavaClassTypeMock

    beforeTest {
        m = SpecialCaseMocks()
        contextClassType = JavaClassTypeMock("MyClass")
        // public method that returns a class and has no parameters
        contextClassType.addPublicMethod(JavaMethodMock("method1", JavaClassTypeMock("method1returntype"), ArrayList()))
        // public method that returns a class and has one parameter
        contextClassType.addPublicMethod(
            JavaMethodMock("method2", JavaClassTypeMock("method2returntype"))
                .addParameter(MethodParameterMock("param1", JavaClassTypeMock("Param1")))
        )
        // public method that returns void and has no parameters
        contextClassType.addPublicMethod(JavaMethodMock("method3", null))
    }

    "can_resolve" {
        val ctx = ValueResolverContext(m.tapestryProject, contextClassType, "prop:method1()", null)
        resolver.execute(ctx) shouldBe true
        (ctx.resultType as IJavaClassType).fullyQualifiedName shouldBe "method1returntype"
        (ctx.resultCodeBind as IJavaMethod).name shouldBe "method1"
    }

    "cant_resolve" {
        for (value in listOf("prop:methodthatdoesnexist()", "prop:method2()", "prop:method3()", "prop:Method1()")) {
            val ctx = ValueResolverContext(m.tapestryProject, contextClassType, value, null)
            resolver.execute(ctx) shouldBe true
            ctx.resultType shouldBe null
        }
    }
})
