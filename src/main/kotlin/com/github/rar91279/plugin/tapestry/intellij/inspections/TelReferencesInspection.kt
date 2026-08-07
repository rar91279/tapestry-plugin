package com.github.rar91279.plugin.tapestry.intellij.inspections

import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.github.rar91279.plugin.tapestry.psi.TelReferenceExpression

/**
 * Reports the TEL references that don't resolve.
 */
class TelReferencesInspection : TapestryInspectionBase() {

    override fun registerProblems(element: PsiElement, holder: ProblemsHolder) {
        if (element !is TelReferenceExpression) return

        val ref = element.reference
        if (!ref.isQualifierResolved()) return

        val results = ref.multiResolve(false)
        val resolvedWithError = results.isNotEmpty() && !results[0].isValidResult

        if (resolvedWithError || results.isEmpty()) { // can not check ref.resolve() for null here as we can have 2 results
            holder.registerProblem(
                ref, ref.getUnresolvedMessage(resolvedWithError),
                if (resolvedWithError) GENERIC_ERROR_OR_WARNING else LIKE_UNKNOWN_SYMBOL
            )
        }
    }

    override fun getShortName(): String = "TelReferencesInspection"
}
