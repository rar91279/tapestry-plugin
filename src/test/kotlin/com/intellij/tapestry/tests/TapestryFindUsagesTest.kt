package com.intellij.tapestry.tests

import com.intellij.lang.findUsages.LanguageFindUsages
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import junit.framework.Assert

/**
 * @author Alexey Chmutov
 */
class TapestryFindUsagesTest : TapestryBaseTestCase() {
    override fun getBasePath(): String {
        return "findUsages/"
    }

    @Throws(Throwable::class)
    fun testAccessorAsPropertyUsage() {
        addComponentToProject("Count")
        doTest("References to a Property", 3)
    }

    @Throws(Throwable::class)
    fun testPropertyAsMethodUsage() {
        addComponentToProject("Count")
        doTest("References to a method", 3)
    }

    private fun doTest(message: String, refsExpected: Int) {
        initByComponent()
        val refs = findUsagesOfElementAtCaret()
        Assert.assertEquals(message, refsExpected, refs.size)
    }

    private fun findUsagesOfElementAtCaret(): Array<PsiReference> {
        val referenceTo = resolveReferenceAtCaretPosition()
        Assert.assertTrue("Wrong FindUsagesProvider", LanguageFindUsages.canFindUsagesFor(referenceTo))
        val scope = GlobalSearchScope.projectScope(myFixture.project)
        val query = if (referenceTo is PsiMethod)
            MethodReferencesSearch.search(referenceTo, scope, false)
        else
            ReferencesSearch.search(referenceTo, scope, true)
        return query.toArray(PsiReference.EMPTY_ARRAY)
    }
}
