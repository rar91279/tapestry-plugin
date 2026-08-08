package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.resolvedName
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.SpecialCaseMocks
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SpecialCaseNumericResolverTest : FreeSpec({
    val resolver = SpecialCaseNumericResolver()
    lateinit var m: SpecialCaseMocks
    beforeTest { m = SpecialCaseMocks() }

    "can_resolve_long" {
        m.expectToFindType("java.lang.Long")

        for (value in listOf("prop: 1 ", " 12345 ")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.resolve(ctx) shouldBe true
            ctx.resultType.resolvedName shouldBe "java.lang.Long"
        }
    }

    "can_resolve_double" {
        m.expectToFindType("java.lang.Double")

        for (value in listOf("prop: 1.1 ", " 1,5 ")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.resolve(ctx) shouldBe true
            ctx.resultType.resolvedName shouldBe "java.lang.Double"
        }
    }

    "cant_resolve_long" {
        for (value in listOf("a", "1,1.1", "1t1")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.resolve(ctx) shouldBe false
            ctx.resultType shouldBe null
        }
    }
})
