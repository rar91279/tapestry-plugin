package com.github.rar91279.plugin.tapestry.tests.core

import com.github.rar91279.plugin.tapestry.core.util.attributeValues
import com.intellij.openapi.application.readActionBlocking
import com.intellij.psi.search.GlobalSearchScope
import io.kotest.matchers.shouldBe

/**
 * Direct coverage of [attributeValues], the one PSI extension of this migration that is not a thin
 * delegation: it has to fold literals and literal-initialised field references into strings, cope with
 * both the single-value and the array-initializer form, and report an unnamed `@Foo("x")` value under
 * `"value"` — the name PSI gives it, and the case the old `IJavaAnnotation.parameters` map got wrong by
 * filing it under a `null` key that nothing ever read.
 */
class PsiExtensionsTest : JavaModuleFixtureSpec({

    fun class1() = javaFacade().findClass("com.app.util.Class1", GlobalSearchScope.moduleRuntimeScope(module, false))!!

    "attributeValues_undeclared_attribute_is_empty" {
        readActionBlocking {
            // @Deprecated — no attributes at all
            class1().modifierList!!.annotations[0].attributeValues("value") shouldBe emptyList()
        }
    }

    "attributeValues_unnamed_value_is_keyed_value" {
        readActionBlocking {
            // @SuppressWarnings("warning1")
            class1().modifierList!!.annotations[1].attributeValues("value") shouldBe listOf("warning1")
        }
    }

    "attributeValues_array_initializer" {
        readActionBlocking {
            class1().fields[0].modifierList!!.annotations[0].attributeValues("parameters").size shouldBe 3
        }
    }
})
