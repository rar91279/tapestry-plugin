package com.github.rar91279.plugin.tapestry.intellij.inspections

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement

/**
 * Silences generic XML inspections that make no sense in a Tapestry template.
 *
 * A template is parsed as XML but authored as HTML: `<i class="fa"></i>` or `<t:zone></t:zone>` must keep
 * its end tag, so "XML tag has empty body" is pure noise here.
 */
class TmlInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean = toolId in SUPPRESSED

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
private val SUPPRESSED = setOf("CheckTagEmptyBody")
