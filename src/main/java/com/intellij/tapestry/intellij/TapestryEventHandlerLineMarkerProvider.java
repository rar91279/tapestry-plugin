package com.intellij.tapestry.intellij;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.java.ultimate.icons.JavaUltimateIcons;
import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.tapestry.intellij.TapestryImplicitUsageProvider.FrameworkMethodKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Marks framework-invoked Tapestry methods (event handlers, render-phase, page lifecycle, IoC
 * contributions) with a gutter icon.
 */
public class TapestryEventHandlerLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // Anchor on the method name identifier (a leaf), per platform guidance.
        if (!(element instanceof PsiIdentifier) || !(element.getParent() instanceof PsiMethod method)) return null;
        if (method.getNameIdentifier() != element) return null;
        FrameworkMethodKind kind = TapestryImplicitUsageProvider.frameworkMethodKind(method);
        if (kind == null) return null;

        Icon icon = switch (kind) {
            case EVENT -> JavaUltimateIcons.Cdi.Gutter.Listener;
            case LIFECYCLE -> JavaUltimateIcons.Cdi.Gutter.ScheduledEvent;
            case IOC -> JavaUltimateIcons.Cdi.Gutter.BeanFactory;
        };
        String tooltip = switch (kind) {
            case EVENT -> "Tapestry event handler";
            case LIFECYCLE -> "Tapestry lifecycle method";
            case IOC -> "Tapestry IoC contribution";
        };
        return new LineMarkerInfo<>(
                element, element.getTextRange(), icon,
                psi -> annotationDoc(method, kind, tooltip), null,
                GutterIconRenderer.Alignment.LEFT, () -> tooltip);
    }

    /**
     * Tooltip = the javadoc of the Tapestry annotation on the method (rendered lazily, only on hover).
     * Falls back to the static text for methods recognized by name alone (annotation optional in Tapestry).
     */
    private static String annotationDoc(PsiMethod method, FrameworkMethodKind kind, String fallback) {
        java.util.List<String> relevant = TapestryImplicitUsageProvider.annotationsForKind(kind);
        for (PsiAnnotation annotation : method.getAnnotations()) {
            String fqn = annotation.getQualifiedName();
            if (fqn == null || !relevant.contains(fqn)) continue;
            PsiClass annotationType = annotation.resolveAnnotationType();
            if (annotationType == null) continue;
            String doc = new JavaDocumentationProvider().generateDoc(annotationType, method);
            if (doc != null) return doc;
        }
        return fallback;
    }
}
