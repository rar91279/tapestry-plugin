package com.github.rar91279.plugin.tapestry.intellij

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.java.ultimate.icons.JavaUltimateIcons
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

/**
 * Marks framework-invoked Tapestry methods (event handlers, render-phase, page lifecycle, IoC
 * contributions) with a gutter icon.
 */
class TapestryEventHandlerLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Anchor on the method name identifier (a leaf), per platform guidance.
        if (element !is PsiIdentifier) return null
        val method = element.parent as? PsiMethod ?: return null
        if (method.nameIdentifier != element) return null
        val kind = frameworkMethodKind(method) ?: return null

        val icon = when (kind) {
            FrameworkMethodKind.EVENT -> JavaUltimateIcons.Cdi.Gutter.Listener
            FrameworkMethodKind.LIFECYCLE -> JavaUltimateIcons.Cdi.Gutter.ScheduledEvent
            FrameworkMethodKind.IOC -> JavaUltimateIcons.Cdi.Gutter.BeanFactory
        }
        val tooltip = when (kind) {
            FrameworkMethodKind.EVENT -> "Tapestry event handler"
            FrameworkMethodKind.LIFECYCLE -> "Tapestry lifecycle method"
            FrameworkMethodKind.IOC -> "Tapestry IoC contribution"
        }
        return LineMarkerInfo(
            element, element.textRange, icon,
            { annotationDoc(method, kind, tooltip) }, null,
            GutterIconRenderer.Alignment.LEFT, { tooltip })
    }

    /**
     * Tooltip = the javadoc of the Tapestry annotation on the method (rendered lazily, only on hover).
     * Falls back to the static text for methods recognized by name alone (annotation optional in Tapestry).
     */
    private fun annotationDoc(method: PsiMethod, kind: FrameworkMethodKind, fallback: String): String {
        for (annotation in method.annotations) {
            if (annotation.qualifiedName !in kind.annotations) continue
            val annotationType = annotation.resolveAnnotationType() ?: continue
            JavaDocumentationProvider().generateDoc(annotationType, method)?.let { return it }
        }
        return fallback
    }
}
