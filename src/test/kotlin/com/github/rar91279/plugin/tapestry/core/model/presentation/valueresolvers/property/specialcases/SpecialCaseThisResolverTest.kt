package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.github.rar91279.plugin.tapestry.core.mocks.psiClassMock
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.resolvedName
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.SpecialCaseMocks
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SpecialCaseThisResolverTest : FreeSpec({
    val resolver = SpecialCaseThisResolver()
    lateinit var m: SpecialCaseMocks
    beforeTest { m = SpecialCaseMocks() }

    "can_resolve" {
        for (value in listOf("prop:this", " THIS ")) {
            val ctx = ValueResolverContext(m.tapestryProject, psiClassMock("myComponentName"), value, null)
            resolver.resolve(ctx) shouldBe true
            ctx.resultType.resolvedName shouldBe "myComponentName"
        }
    }

    "cant_resolve" {
        val ctx = ValueResolverContext(m.tapestryProject, null, "this1", null)
        resolver.resolve(ctx) shouldBe false
        ctx.resultType shouldBe null
    }
})
