package com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.intellij.psi.CommonClassNames
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.resolvedName
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.github.rar91279.plugin.tapestry.core.model.presentation.valueresolvers.property.SpecialCaseMocks
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SpecialCaseNullResolverTest : FreeSpec({
    val resolver = SpecialCaseNullResolver()
    lateinit var m: SpecialCaseMocks
    beforeTest { m = SpecialCaseMocks() }

    "can_resolve" {
        m.expectToFindType(CommonClassNames.JAVA_LANG_OBJECT)

        for (value in listOf("prop:null", " NULL ")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.resolve(ctx) shouldBe true
            ctx.resultType.resolvedName shouldBe CommonClassNames.JAVA_LANG_OBJECT
        }
    }

    "cant_resolve" {
        val ctx = ValueResolverContext(m.tapestryProject, null, "null1", null)
        resolver.resolve(ctx) shouldBe false
        ctx.resultType shouldBe null
    }
})
