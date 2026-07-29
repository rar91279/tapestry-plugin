package com.intellij.tapestry.intellij;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.java.ultimate.icons.JavaUltimateIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
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
                psi -> tooltip, null,
                GutterIconRenderer.Alignment.LEFT, () -> tooltip);
    }
}
