package com.intellij.tapestry.core.model.presentation.valueresolvers.property.specialcases

import com.intellij.psi.CommonClassNames
import com.intellij.tapestry.core.java.IJavaClassType
import com.intellij.tapestry.core.mocks.JavaClassTypeMock
import com.intellij.tapestry.core.model.presentation.valueresolvers.ValueResolverContext
import com.intellij.tapestry.core.model.presentation.valueresolvers.property.SpecialCaseMocks
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SpecialCaseNullResolverTest : FreeSpec({
    val resolver = SpecialCaseNullResolver()
    lateinit var m: SpecialCaseMocks
    beforeTest { m = SpecialCaseMocks() }

    "can_resolve" {
        m.expectToFindType(CommonClassNames.JAVA_LANG_OBJECT, JavaClassTypeMock(CommonClassNames.JAVA_LANG_OBJECT))

        for (value in listOf("prop:null", " NULL ")) {
            val ctx = ValueResolverContext(m.tapestryProject, null, value, null)
            resolver.execute(ctx) shouldBe true
            (ctx.resultType as IJavaClassType).fullyQualifiedName shouldBe CommonClassNames.JAVA_LANG_OBJECT
        }
    }

    "cant_resolve" {
        val ctx = ValueResolverContext(m.tapestryProject, null, "null1", null)
        resolver.execute(ctx) shouldBe false
        ctx.resultType shouldBe null
    }
})
