package com.intellij.tapestry.tests.mocks

import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

class PsiClassTypeMock : PsiClassType(LanguageLevel.JDK_1_5) {

    private var _resolve: PsiClass? = null

    override fun resolve(): PsiClass? = _resolve

    fun setResolve(resolve: PsiClass?): PsiClassTypeMock {
        _resolve = resolve
        return this
    }

    override fun getClassName(): String? = null

    override fun getParameters(): Array<PsiType> = PsiType.EMPTY_ARRAY

    override fun resolveGenerics(): PsiClassType.ClassResolveResult = throw UnsupportedOperationException()

    override fun rawType(): PsiClassType = throw UnsupportedOperationException()

    override fun getResolveScope(): GlobalSearchScope = throw UnsupportedOperationException()

    override fun getLanguageLevel(): LanguageLevel = throw UnsupportedOperationException()

    override fun setLanguageLevel(languageLevel: LanguageLevel): PsiClassType = this

    override fun getPresentableText(): String = getCanonicalText()

    override fun getCanonicalText(): String = "?"

    override fun isValid(): Boolean = false

    override fun equalsToText(text: String): Boolean = false
}
