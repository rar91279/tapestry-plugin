package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.resolvedName
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.SpecialCaseMocks
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SpecialCaseBooleanResolverTest : FreeSpec({
    val resolver = SpecialCaseBooleanResolver()
    lateinit var m: SpecialCaseMocks
    beforeTest { m = SpecialCaseMocks() }

    "can_resolve" {
        m.expectToFindType("java.lang.Boolean")

        for (value in listOf("true", "prop:false", " FALSE ")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.resolve(ctx) shouldBe true
            ctx.resultType.resolvedName shouldBe "java.lang.Boolean"
        }
    }

    "cant_resolve" {
        val ctx = ValueResolverContext(m.tapestryProject, null, "true1", null)
        resolver.resolve(ctx) shouldBe false
        ctx.resultType shouldBe null
    }
})
