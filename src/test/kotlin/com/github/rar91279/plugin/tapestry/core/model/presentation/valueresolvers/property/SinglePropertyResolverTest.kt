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

class SinglePropertyResolverTest : FreeSpec({
    val resolver = SinglePropertyResolver()
    lateinit var m: SpecialCaseMocks
    lateinit var contextClassType: PsiClass

    beforeTest {
        m = SpecialCaseMocks()
        // public getter that returns a class and has no parameters
        contextClassType = psiClassMock("MyClass").stubMethods(
            psiMethodMock("getProp1", returnType = psiClassTypeMock(psiClassMock("prop1returntype")))
        )
    }

    "can_resolve" {
        for (value in listOf("prop:prop1", "prop:pRoP1")) {
            val ctx = ValueResolverContext(m.tapestryProject, contextClassType, value, null)
            resolver.resolve(ctx) shouldBe true
            ctx.resultType.resolvedName shouldBe "prop1returntype"
            (ctx.resultCodeBind as PsiMethod).name shouldBe "getProp1"
        }
    }

    "cant_resolve" {
        val ctx = ValueResolverContext(m.tapestryProject, contextClassType, "prop:propthatdoesnexist", null)
        resolver.resolve(ctx) shouldBe true
        ctx.resultType shouldBe null
    }
})
