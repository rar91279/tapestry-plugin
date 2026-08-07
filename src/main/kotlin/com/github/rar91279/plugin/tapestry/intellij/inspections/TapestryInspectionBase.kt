package com.github.rar91279.plugin.tapestry.intellij.inspections

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.github.rar91279.plugin.tapestry.TapestryBundle

/**
 * Base class of the Tapestry inspections: visits every element and delegates to [registerProblems].
 */
abstract class TapestryInspectionBase : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                registerProblems(element, holder)
            }
        }

    protected abstract fun registerProblems(element: PsiElement, holder: ProblemsHolder)

    override fun getGroupDisplayName(): String = TapestryBundle.message("tapestry.inspections.group")

    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevel.WARNING

    override fun isEnabledByDefault(): Boolean = true
}
