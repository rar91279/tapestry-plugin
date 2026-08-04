package com.github.rar91279.plugin.tapestry.psi

import com.intellij.html.embedding.HtmlEmbeddedContentProvider
import com.intellij.html.embedding.HtmlEmbeddedContentSupport
import com.intellij.html.embedding.HtmlTokenEmbeddedContentProvider
import com.intellij.lexer.BaseHtmlLexer
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.XHtmlLexer
import com.intellij.lexer.XmlLexer
import com.intellij.lexer._XmlLexer
import com.intellij.lexer.__XmlLexer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.xml.util.HtmlUtil.STYLE_ATTRIBUTE_NAME

/** Lexer of the Tapestry Expression Language. */
class TelLexer : FlexAdapter(_TelLexer())

/** Lexer of a Tapestry template: XHTML with Tapestry-EL aware attribute values. */
class TmlLexer : XHtmlLexer(createElAwareXmlLexer()) {

    override fun isAttributeEmbedmentToken(tokenType: IElementType, attributeName: CharSequence): Boolean =
        if (tokenType === TelTokenTypes.TAP5_EL_CONTENT) !StringUtil.equals(attributeName, STYLE_ATTRIBUTE_NAME)
        else super.isAttributeEmbedmentToken(tokenType, attributeName)

    companion object {

        @JvmStatic
        fun createElAwareXmlLexer(): XmlLexer {
            val internalLexer = __XmlLexer(null)
            internalLexer.setElTypes(TelTokenTypes.TAP5_EL_CONTENT, TelTokenTypes.TAP5_EL_CONTENT)

            return XmlLexer(_XmlLexer(internalLexer))
        }
    }
}

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
