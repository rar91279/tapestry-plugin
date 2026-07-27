package com.intellij.tapestry.lang

import com.intellij.ide.highlighter.XmlFileHighlighter
import com.intellij.lexer.Lexer
import com.intellij.psi.tree.IElementType
import com.intellij.tapestry.psi.TelTokenType
import com.intellij.tapestry.psi.TmlHighlightingLexer

/**
 * Syntax highlighter for Tapestry Template Language (TML) files.
 *
 * TML files are XML-based templates that can contain Tapestry Expression Language (TEL) expressions.
 * This highlighter extends [XmlFileHighlighter] to provide XML syntax highlighting and delegates
 * TEL token highlighting to [TelHighlighter].
 *
 * @author Alexey Chmutov
 */
class TmlHighlighter : XmlFileHighlighter() {
    /**
     * Lazy-initialized highlighter for Tapestry Expression Language (TEL) tokens.
     */
    val highlighter by lazy {
        TelHighlighter()
    }

    /**
     * Returns the lexer used for syntax highlighting of TML files.
     *
     * @return a [TmlHighlightingLexer] instance that can tokenize both XML and TEL syntax
     */
    override fun getHighlightingLexer(): Lexer {
        return TmlHighlightingLexer()
    }

    /**
     * Returns the text attributes for the specified token type.
     *
     * Delegates TEL token highlighting to [TelHighlighter] and XML token highlighting to the parent
     * [XmlFileHighlighter].
     *
     * @param tokenType the token type to get highlighting attributes for
     * @return an array of [com.intellij.openapi.editor.colors.TextAttributesKey] for the token type
     */
    override fun getTokenHighlights(tokenType: IElementType) =
        if (tokenType is TelTokenType)
            highlighter.getTokenHighlights(tokenType)
        else
            super.getTokenHighlights(tokenType)
}
