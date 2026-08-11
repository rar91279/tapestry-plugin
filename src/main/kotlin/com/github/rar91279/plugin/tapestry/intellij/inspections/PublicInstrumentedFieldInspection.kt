package com.github.rar91279.plugin.tapestry.intellij.inspections

import com.github.rar91279.plugin.tapestry.TapestryBundle
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier

/**
 * Reports the public fields Tapestry has to instrument.
 *
 * Tapestry replaces access to such a field with generated accessors, which it can only do while nothing
 * outside the class reads the field directly. A public one fails the page at runtime with
 * "Field <name> of class <component> must be instrumented, and may not be public".
 *
 * Only `public` is reported — a `protected` field in a base component is instrumented fine, and the
 * plugin's own model accepts it (see `ParameterReceiverElement.parameters`).
 */
class PublicInstrumentedFieldInspection : TapestryInspectionBase() {

    override fun registerProblems(element: PsiElement, holder: ProblemsHolder) {
        if (element !is PsiField || !element.hasModifierProperty(PsiModifier.PUBLIC)) return

        val annotation = INSTRUMENTED_ANNOTATIONS.firstOrNull { element.hasAnnotation(it) } ?: return

        holder.registerProblem(
            element.nameIdentifier,
            TapestryBundle.message("public.instrumented.field.inspection.message", annotation.substringAfterLast('.'))
        )
    }

    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevel.ERROR

    override fun getShortName(): String = "PublicInstrumentedField"

    private companion object {

        /** The field annotations that make Tapestry instrument the field. */
        val INSTRUMENTED_ANNOTATIONS = listOf(
            TapestryConstants.PROPERTY_ANNOTATION,
            "org.apache.tapestry5.annotations.Parameter",
            "org.apache.tapestry5.annotations.Persist",
            "org.apache.tapestry5.annotations.SessionState",
            "org.apache.tapestry5.annotations.SessionAttribute",
            "org.apache.tapestry5.annotations.ApplicationState",
            "org.apache.tapestry5.annotations.Environmental",
            "org.apache.tapestry5.annotations.ActivationRequestParameter",
            "org.apache.tapestry5.annotations.InjectComponent",
            "org.apache.tapestry5.annotations.InjectContainer",
            TapestryConstants.COMPONENT_ANNOTATION,
            TapestryConstants.INJECT_PAGE_ANNOTATION,
            TapestryConstants.MIXIN_ANNOTATION,
            // Injection: Tapestry's own annotation, plus the JSR-330 one it honours since 5.9.
            TapestryConstants.CORE_INJECT_ANNOTATION,
            "org.apache.tapestry5.ioc.annotations.Inject",
            "jakarta.inject.Inject",
            "javax.inject.Inject",
        )
    }
}
