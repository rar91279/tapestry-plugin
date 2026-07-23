package com.intellij.tapestry.intellij.lang.reference;

import com.intellij.codeInsight.daemon.impl.analysis.PsiReferenceWithUnresolvedQuickFixes;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;

/**
 * A {@link PsiReferenceBase} that opts into unresolved-reference error highlighting.
 * <p>
 * Since IntelliJ 2026.2, {@code XmlHighlightVisitor.shouldCheckResolve} only resolve-checks XML
 * references that implement {@link PsiReferenceWithUnresolvedQuickFixes} (or a handful of built-in
 * platform reference types). Plain {@code PsiReferenceBase} instances are no longer highlighted when
 * unresolved, so the Tapestry references must carry this marker to keep flagging unknown
 * component/page/type/id references.
 */
public abstract class TapestryPsiReferenceBase<T extends PsiElement> extends PsiReferenceBase<T>
  implements PsiReferenceWithUnresolvedQuickFixes {

  public TapestryPsiReferenceBase(T element, TextRange range) {
    super(element, range);
  }
}
