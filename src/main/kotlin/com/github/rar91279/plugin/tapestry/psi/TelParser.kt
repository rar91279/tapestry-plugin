package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.IElementType
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.ARGUMENT_LIST
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.BOOLEAN_LITERAL
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.DECIMAL_LITERAL
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.EXPLICIT_BINDING
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.INTEGER_LITERAL
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.LIST_EXPRESSION
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.METHOD_CALL_EXPRESSION
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.NOT_OP_EXPRESSION
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.NULL_LITERAL
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.RANGE_EXPRESSION
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.REFERENCE_EXPRESSION
import com.github.rar91279.plugin.tapestry.psi.TelCompositeElementType.Companion.STRING_LITERAL
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_CONTEXT_NODE_KEY
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_BOOLEAN
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_COLON
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_COMMA
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_DECIMAL
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_DOT
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_END
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_EXCLAMATION
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_IDENTIFIER
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_INTEGER
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_LEFT_BRACKET
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_LEFT_PARENTH
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_NULL
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_QUESTION_DOT
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_RANGE
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_RIGHT_BRACKET
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_RIGHT_PARENTH
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_START
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TAP5_EL_STRING
import com.github.rar91279.plugin.tapestry.psi.TelTokenTypes.TEL_FILE

/**
 * Parser of the Tapestry Expression Language.
 */
class TelParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val contextNode = builder.getUserData(TAP5_CONTEXT_NODE_KEY)
        val rootMarker = builder.mark()
        val elUnderFile = contextNode != null && contextNode.elementType === TEL_FILE
        val markerUnderFile = if (elUnderFile) builder.mark() else null

        while (!builder.eof()) {
            parseExpression(builder)
        }

        if (markerUnderFile != null) {
            markerUnderFile.done(root)
            rootMarker.done(TEL_FILE)
        }
        else {
            rootMarker.done(root)
        }

        return builder.treeBuilt
    }

    companion object {

        private val LAST_FOUND_IDENT: Key<String> = Key.create("LAST_FOUND_IDENT")

        fun parseExpression(builder: PsiBuilder) {
            if (!consumeToken(builder, TAP5_EL_START)) {
                builder.advanceLexer()
                return
            }

            val referenceExpression = tryToConsumeIdentifierAndMark(builder)
            if (referenceExpression == null) {
                parseExpressionInner(builder)
            }
            else if (consumeOptionalToken(builder, TAP5_EL_COLON)) {
                try {
                    if ("prop".equals(builder.getUserData(LAST_FOUND_IDENT), ignoreCase = true)) {
                        parseExpressionInner(builder)
                    }
                    else {
                        while (TAP5_EL_END !== builder.tokenType) builder.advanceLexer()
                    }
                }
                finally {
                    referenceExpression.done(EXPLICIT_BINDING)
                    builder.putUserData(LAST_FOUND_IDENT, null)
                }
            }
            else {
                parsePropertyChainTrailer(builder, referenceExpression)
            }

            if (!consumeToken(builder, TAP5_EL_END)) {
                while (!builder.eof() && builder.tokenType !== TAP5_EL_END && builder.tokenType !== TAP5_EL_START) {
                    builder.advanceLexer()
                }
                consumeOptionalToken(builder, TAP5_EL_END)
            }
        }

        private fun parseExpressionInner(builder: PsiBuilder): Boolean {
            var mark = builder.mark()
            var res: TelCompositeElementType? = null

            try {
                if (consumeOptionalToken(builder, TAP5_EL_LEFT_BRACKET)) {
                    res = LIST_EXPRESSION
                    parseExpressionList(builder)
                    consumeToken(builder, TAP5_EL_RIGHT_BRACKET)
                    return true
                }
                if (consumeOptionalToken(builder, TAP5_EL_EXCLAMATION)) {
                    res = NOT_OP_EXPRESSION
                    parseExpressionInner(builder)
                    return true
                }

                res = parseConstantExpr(builder)
                var propertyChainFound = false
                if (res == null) {
                    propertyChainFound = parsePropertyChainExpression(builder)
                }

                if ((propertyChainFound || res === INTEGER_LITERAL) && builder.tokenType === TAP5_EL_RANGE) {
                    if (res != null) {
                        mark.done(res)
                        mark = mark.precede()
                    }
                    consumeToken(builder, TAP5_EL_RANGE)
                    res = RANGE_EXPRESSION
                    if (!parseIntegerLiteral(builder) && !parsePropertyChainExpression(builder)) {
                        builder.error("property chain or integer literal expected")
                    }
                }

                return propertyChainFound || res != null
            }
            finally {
                if (res != null) mark.done(res) else mark.drop()
            }
        }

        private fun parseIntegerLiteral(builder: PsiBuilder): Boolean {
            val mark = builder.mark()
            val result = consumeOptionalToken(builder, TAP5_EL_INTEGER)

            if (result) mark.done(INTEGER_LITERAL) else mark.drop()

            return result
        }

        private fun consumeOptionalToken(builder: PsiBuilder, tokenType: TelTokenType): Boolean {
            if (tokenType !== builder.tokenType) return false

            builder.advanceLexer()
            return true
        }

        private fun consumeToken(builder: PsiBuilder, tokenType: TelTokenType): Boolean {
            if (tokenType !== builder.tokenType) {
                builder.error(StringUtil.trimStart(tokenType.toString(), "TAP5_EL_") + " expected")
                return false
            }

            builder.advanceLexer()
            return true
        }

        private fun parseConstantExpr(builder: PsiBuilder): TelCompositeElementType? = when {
            consumeOptionalToken(builder, TAP5_EL_BOOLEAN) -> BOOLEAN_LITERAL
            consumeOptionalToken(builder, TAP5_EL_INTEGER) -> INTEGER_LITERAL
            consumeOptionalToken(builder, TAP5_EL_DECIMAL) -> DECIMAL_LITERAL
            consumeOptionalToken(builder, TAP5_EL_STRING) -> STRING_LITERAL
            consumeOptionalToken(builder, TAP5_EL_NULL) -> NULL_LITERAL
            else -> null
        }

        private fun parsePropertyChainExpression(builder: PsiBuilder): Boolean {
            val referenceExpression = tryToConsumeIdentifierAndMark(builder) ?: return false

            parsePropertyChainTrailer(builder, referenceExpression)
            return true
        }

        private fun tryToConsumeIdentifierAndMark(builder: PsiBuilder): PsiBuilder.Marker? {
            if (TAP5_EL_IDENTIFIER !== builder.tokenType) return null

            val mark = builder.mark()
            builder.putUserData(LAST_FOUND_IDENT, builder.tokenText)
            builder.advanceLexer()

            return mark
        }

        private fun parsePropertyChainTrailer(builder: PsiBuilder, referenceExpression: PsiBuilder.Marker) {
            referenceExpression.done(REFERENCE_EXPRESSION)
            var current = parseMethodCallArgumentList(builder, referenceExpression).precede()

            while (consumeOptionalToken(builder, TAP5_EL_DOT) || consumeOptionalToken(builder, TAP5_EL_QUESTION_DOT)) {
                if (!consumeToken(builder, TAP5_EL_IDENTIFIER)) break

                current.done(REFERENCE_EXPRESSION)
                current = parseMethodCallArgumentList(builder, current).precede()
            }

            current.drop()
        }

        private fun parseMethodCallArgumentList(builder: PsiBuilder, referenceExpression: PsiBuilder.Marker): PsiBuilder.Marker {
            if (!consumeOptionalToken(builder, TAP5_EL_LEFT_PARENTH)) return referenceExpression

            val methodCall = referenceExpression.precede()
            val mark = builder.mark()
            try {
                parseExpressionList(builder)
            }
            finally {
                mark.done(ARGUMENT_LIST)
            }

            consumeToken(builder, TAP5_EL_RIGHT_PARENTH)
            methodCall.done(METHOD_CALL_EXPRESSION)

            return methodCall
        }

        private fun parseExpressionList(builder: PsiBuilder) {
            if (!parseExpressionInner(builder)) return

            while (consumeOptionalToken(builder, TAP5_EL_COMMA)) {
                if (!parseExpressionInner(builder)) {
                    builder.error("expression expected")
                }
            }
        }
    }
}
