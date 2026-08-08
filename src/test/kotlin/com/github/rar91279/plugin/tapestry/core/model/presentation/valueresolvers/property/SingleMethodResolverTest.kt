package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property

import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiClassTypeMock
import com.github.rar91279.plugin.tapestry.core.mocks.psiMethodMock
import com.github.rar91279.plugin.tapestry.core.mocks.stubMethods
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SingleMethodResolverTest : FreeSpec({
    val resolver = SingleMethodResolver()
    lateinit var m: SpecialCaseMocks
    lateinit var contextClassType: PsiClass

    beforeTest {
        m = SpecialCaseMocks()
        contextClassType = psiClassMock("MyClass").stubMethods(
            // public method that returns a class and has no parameters
            psiMethodMock("method1", returnType = psiClassTypeMock(psiClassMock("method1returntype"))),
            // public method that returns a class and has one parameter
            psiMethodMock("method2", returnType = psiClassTypeMock(psiClassMock("method2returntype")), parameterCount = 1),
            // public method that returns void and has no parameters
            psiMethodMock("method3")
        )
    }

    "can_resolve" {
        val ctx = ValueResolverContext(m.tapestryProject, contextClassType, "prop:method1()", null)
        resolver.resolve(ctx) shouldBe true
        ctx.resultType.resolvedName shouldBe "method1returntype"
        (ctx.resultCodeBind as PsiMethod).name shouldBe "method1"
    }

    "cant_resolve" {
        for (value in listOf("prop:methodthatdoesnexist()", "prop:method2()", "prop:method3()", "prop:Method1()")) {
            val ctx = ValueResolverContext(m.tapestryProject, contextClassType, value, null)
            resolver.resolve(ctx) shouldBe true
            ctx.resultType shouldBe null
        }
    }
})
