package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lexer.XHtmlLexer
import com.intellij.psi.tree.TokenSet

/**
 * Highlighting lexer of a Tapestry template.
 *
 * XHtmlHighlightingLexer was removed in 2026.2; XHtmlLexer in highlight mode is its replacement.
 * Being a BaseHtmlLexer, it drives HtmlEmbeddedContentSupport (TmlEmbeddedContentSupport) so the
 * Tapestry-EL content is sub-lexed with TelLexer for highlighting.
 */
class TmlHighlightingLexer : XHtmlLexer(TmlLexer.createElAwareXmlLexer(), true) {

    override fun createAttributeEmbedmentTokenSet(): TokenSet =
        TokenSet.orSet(super.createAttributeEmbedmentTokenSet(), CUSTOM_ATTRIBUTE_TOKENS)

    private companion object {
        val CUSTOM_ATTRIBUTE_TOKENS: TokenSet = TokenSet.create(TelTokenTypes.TAP5_EL_CONTENT)
    }
}
