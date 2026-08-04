package com.github.rar91279.plugin.tapestry.intellij.editorActions

import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.github.rar91279.plugin.tapestry.TapestryBundle

class TmlFindUsagesProvider : FindUsagesProvider {

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement is PsiMethod || psiElement is PsiField

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String = TapestryBundle.message("type.name.reference")

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? PsiNamedElement)?.name ?: TapestryBundle.message("type.name.reference")

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        (element as? PsiNamedElement)?.name ?: element.text
}
