package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.mocks.JavaClassTypeMock
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.SpecialCaseMocks
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SpecialCaseLiteralStringResolverTest : FreeSpec({
    val resolver = SpecialCaseLiteralStringResolver()
    lateinit var m: SpecialCaseMocks
    beforeTest { m = SpecialCaseMocks() }

    "can_resolve" {
        m.expectToFindType("java.lang.String", JavaClassTypeMock("java.lang.String"))

        for (value in listOf("prop:'hey'", " ' hey ' ")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.execute(ctx) shouldBe true
            (ctx.resultType as IJavaClassType).fullyQualifiedName shouldBe "java.lang.String"
        }
    }

    "cant_resolve" {
        for (value in listOf("1", "1'hey'")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.execute(ctx) shouldBe false
            ctx.resultType shouldBe null
        }
    }
})
