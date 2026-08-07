package com.github.rar91279.plugin.tapestry.psi

import com.intellij.html.embedding.HtmlEmbeddedContentProvider
import com.intellij.html.embedding.HtmlEmbeddedContentSupport
import com.intellij.html.embedding.HtmlTokenEmbeddedContentProvider
import com.intellij.lexer.BaseHtmlLexer

/** Sub-lexes the Tapestry-EL content of a template with [TelLexer]. */
class TmlEmbeddedContentSupport : HtmlEmbeddedContentSupport {

    override fun isEnabled(lexer: BaseHtmlLexer): Boolean = lexer is TmlLexer || lexer is TmlHighlightingLexer

    override fun createEmbeddedContentProviders(lexer: BaseHtmlLexer): List<HtmlEmbeddedContentProvider> =
        listOf(
            HtmlTokenEmbeddedContentProvider(
                lexer, TelTokenTypes.TAP5_EL_CONTENT, { TelLexer() }, { TelTokenTypes.TAP5_EL_HOLDER }
            )
        )
}
