package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod
import com.github.rar91279.plugin.tapestry.core.mocks.JavaClassTypeMock
import com.github.rar91279.plugin.tapestry.core.mocks.JavaMethodMock
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SinglePropertyResolverTest : FreeSpec({
    val resolver = SinglePropertyResolver()
    lateinit var m: SpecialCaseMocks
    lateinit var contextClassType: JavaClassTypeMock

    beforeTest {
        m = SpecialCaseMocks()
        contextClassType = JavaClassTypeMock("MyClass")
        // public getter that returns a class and has no parameters
        contextClassType.addPublicMethod(JavaMethodMock("getProp1", JavaClassTypeMock("prop1returntype"), ArrayList()))
    }

    "can_resolve" {
        for (value in listOf("prop:prop1", "prop:pRoP1")) {
            val ctx = ValueResolverContext(m.tapestryProject, contextClassType, value, null)
            resolver.execute(ctx) shouldBe true
            (ctx.resultType as IJavaClassType).fullyQualifiedName shouldBe "prop1returntype"
            (ctx.resultCodeBind as IJavaMethod).name shouldBe "getProp1"
        }
    }

    "cant_resolve" {
        val ctx = ValueResolverContext(m.tapestryProject, contextClassType, "prop:propthatdoesnexist", null)
        resolver.execute(ctx) shouldBe true
        ctx.resultType shouldBe null
    }
})
