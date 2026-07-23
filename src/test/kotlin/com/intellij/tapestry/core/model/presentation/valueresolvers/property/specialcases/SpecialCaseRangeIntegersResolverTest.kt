package com.intellij.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.intellij.tapestry.core.model.presentation.valueresolvers.property.SpecialCaseMocks
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SpecialCaseRangeIntegersResolverTest : FreeSpec({
    val resolver = SpecialCaseRangeIntegersResolver()
    lateinit var m: SpecialCaseMocks
    beforeTest { m = SpecialCaseMocks() }

    "can_resolve" {
        m.expectToFindType("java.lang.Iterable", JavaClassTypeMock("java.lang.Iterable"))

        for (value in listOf("prop:1..2", " 12..56 ")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.execute(ctx) shouldBe true
            (ctx.resultType as IJavaClassType).fullyQualifiedName shouldBe "java.lang.Iterable"
        }
    }

    "cant_resolve" {
        for (value in listOf("1", "1...2", "1,,2")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.execute(ctx) shouldBe false
            ctx.resultType shouldBe null
        }
    }
})
