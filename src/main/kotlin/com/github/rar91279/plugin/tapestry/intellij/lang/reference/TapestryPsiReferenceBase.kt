package com.github.rar91279.plugin.tapestry.intellij.lang.reference

import com.intellij.codeInsight.daemon.impl.analysis.PsiReferenceWithUnresolvedQuickFixes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

/**
 * A [PsiReferenceBase] that opts into unresolved-reference error highlighting.
 *
 * Since IntelliJ 2026.2, `XmlHighlightVisitor.shouldCheckResolve` only resolve-checks XML
 * references that implement [PsiReferenceWithUnresolvedQuickFixes] (or a handful of built-in
 * platform reference types). Plain `PsiReferenceBase` instances are no longer highlighted when
 * unresolved, so the Tapestry references must carry this marker to keep flagging unknown
 * component/page/type/id references.
 */
abstract class TapestryPsiReferenceBase<T : PsiElement>(element: T, range: TextRange) :
    PsiReferenceBase<T>(element, range), PsiReferenceWithUnresolvedQuickFixes
