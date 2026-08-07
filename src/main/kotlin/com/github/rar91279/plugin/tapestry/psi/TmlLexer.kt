package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lexer.XHtmlLexer
import com.intellij.lexer.XmlLexer
import com.intellij.lexer._XmlLexer
import com.intellij.lexer.__XmlLexer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.IElementType
import com.intellij.xml.util.HtmlUtil.STYLE_ATTRIBUTE_NAME

/** Lexer of a Tapestry template: XHTML with Tapestry-EL aware attribute values. */
class TmlLexer : XHtmlLexer(createElAwareXmlLexer()) {

    override fun isAttributeEmbedmentToken(tokenType: IElementType, attributeName: CharSequence): Boolean =
        if (tokenType === TelTokenTypes.TAP5_EL_CONTENT) !StringUtil.equals(attributeName, STYLE_ATTRIBUTE_NAME)
        else super.isAttributeEmbedmentToken(tokenType, attributeName)

    companion object {

        fun createElAwareXmlLexer(): XmlLexer {
            val internalLexer = __XmlLexer(null)
            internalLexer.setElTypes(TelTokenTypes.TAP5_EL_CONTENT, TelTokenTypes.TAP5_EL_CONTENT)

            return XmlLexer(_XmlLexer(internalLexer))
        }
    }
}
