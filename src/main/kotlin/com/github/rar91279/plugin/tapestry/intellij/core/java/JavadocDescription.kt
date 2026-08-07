package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiWhiteSpace

/**
 * The javadoc description of this element, empty when it has none. Falls back to the navigation
 * element, whose language may be one without javadoc (e.g. a Kotlin `KtProperty`).
 */
internal fun PsiDocCommentOwner.javadocDescription(): String {
    val docComment = docComment ?: (navigationElement as? PsiDocCommentOwner)?.docComment ?: return ""

    return docComment.descriptionElements
        .filter { it !is PsiWhiteSpace }
        .joinToString("") { it.text }
}
