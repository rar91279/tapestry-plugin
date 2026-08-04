package com.github.rar91279.plugin.tapestry.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.github.rar91279.plugin.tapestry.intellij.lang.TemplateColorSettingsPage
import com.github.rar91279.plugin.tapestry.psi.TelLexer
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes
import kotlin.collections.get

/**
 * Syntax highlighter for Tapestry Expression Language (TEL).
 * 
 * Provides syntax highlighting for TEL expressions used in Tapestry templates,
 * mapping token types from the TEL lexer to text attribute keys defined in
 * [TemplateColorSettingsPage].
 * 
 * @author Alexey Chmutov
 */
class TelHighlighter : SyntaxHighlighterBase() {

    /**
     * Lazy-initialized map that associates TEL token types with their corresponding
     * text attribute keys for syntax highlighting.
     */
    private val ourMap: MutableMap<IElementType, TextAttributesKey> by lazy {
        val map = mutableMapOf<IElementType, TextAttributesKey>()
        fillMap(map, TemplateColorSettingsPage.TEL_BOUNDS, TelTokenTypes.TAP5_EL_START, TelTokenTypes.TAP5_EL_END)
        fillMap(map, TemplateColorSettingsPage.TEL_IDENT, TelTokenTypes.TAP5_EL_IDENTIFIER)
        fillMap(map, TemplateColorSettingsPage.TEL_NUMBER, TelTokenTypes.TAP5_EL_INTEGER, TelTokenTypes.TAP5_EL_DECIMAL)
        fillMap(map, TemplateColorSettingsPage.TEL_DOT, TelTokenTypes.TAP5_EL_DOT, TelTokenTypes.TAP5_EL_COLON, TelTokenTypes.TAP5_EL_COMMA, TelTokenTypes.TAP5_EL_QUESTION_DOT, TelTokenTypes.TAP5_EL_RANGE, TelTokenTypes.TAP5_EL_EXCLAMATION)
        fillMap(map, TemplateColorSettingsPage.TEL_PARENTHS, TelTokenTypes.TAP5_EL_LEFT_PARENTH, TelTokenTypes.TAP5_EL_RIGHT_PARENTH)
        fillMap(map, TemplateColorSettingsPage.TEL_BRACKETS, TelTokenTypes.TAP5_EL_LEFT_BRACKET, TelTokenTypes.TAP5_EL_RIGHT_BRACKET)
        fillMap(map, TemplateColorSettingsPage.TEL_STRING, TelTokenTypes.TAP5_EL_STRING)
        fillMap(map, TemplateColorSettingsPage.TEL_BAD_CHAR, TelTokenTypes.TAP5_EL_BAD_CHAR)
        return@lazy map
    }

    /**
     * Returns the lexer used for highlighting TEL expressions.
     *
     * @return a new instance of [TelLexer]
     */
    override fun getHighlightingLexer(): Lexer = TelLexer()

    /**
     * Returns the text attributes to apply to the given token type.
     *
     * @param tokenType the token type to get highlighting attributes for
     * @return an array of text attribute keys, including the token-specific attribute
     *         and the TEL background attribute
     */
    override fun getTokenHighlights(tokenType: IElementType?): Array<out TextAttributesKey>
        = pack(ourMap[tokenType], TemplateColorSettingsPage.TEL_BACKGROUND)
}
