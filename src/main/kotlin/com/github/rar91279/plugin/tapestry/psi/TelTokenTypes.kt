package com.github.rar91279.plugin.tapestry.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.ILazyParseableElementType
import com.intellij.psi.tree.TokenSet
import com.github.rar91279.plugin.tapestry.lang.TelFileType
import com.github.rar91279.plugin.tapestry.lang.TelLanguage
import com.github.rar91279.plugin.tapestry.lang.TmlLanguage

/** A token of the Tapestry Expression Language. */
class TelTokenType(debugName: String) : IElementType(debugName, TelFileType.language) {

    fun createPsiElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
}

/** The file element type of a Tapestry template. */
object TmlElementType {

    @JvmField
    val TML_FILE: IFileElementType = IFileElementType("TML_FILE", TmlLanguage)
}

/** The token types of the Tapestry Expression Language. */
object TelTokenTypes {

    @JvmField
    val TEL_FILE: IFileElementType = IFileElementType(TelLanguage.INSTANCE)

    @JvmField
    val TAP5_EL_START: TelTokenType = TelTokenType("\${")

    @JvmField
    val TAP5_EL_END: TelTokenType = TelTokenType("}")

    @JvmField
    val TAP5_EL_IDENTIFIER: TelTokenType = TelTokenType("TAP5_EL_IDENTIFIER")

    @JvmField
    val TAP5_EL_DOT: TelTokenType = TelTokenType(".")

    @JvmField
    val TAP5_EL_COLON: TelTokenType = TelTokenType(":")

    @JvmField
    val TAP5_EL_COMMA: TelTokenType = TelTokenType(",")

    @JvmField
    val TAP5_EL_QUESTION_DOT: TelTokenType = TelTokenType("?.")

    @JvmField
    val TAP5_EL_RANGE: TelTokenType = TelTokenType("..")

    @JvmField
    val TAP5_EL_EXCLAMATION: TelTokenType = TelTokenType("!")

    @JvmField
    val TAP5_EL_LEFT_PARENTH: TelTokenType = TelTokenType("(")

    @JvmField
    val TAP5_EL_RIGHT_PARENTH: TelTokenType = TelTokenType(")")

    @JvmField
    val TAP5_EL_LEFT_BRACKET: TelTokenType = TelTokenType("[")

    @JvmField
    val TAP5_EL_RIGHT_BRACKET: TelTokenType = TelTokenType("]")

    @JvmField
    val TAP5_EL_STRING: TelTokenType = TelTokenType("TAP5_EL_STRING")

    @JvmField
    val TAP5_EL_INTEGER: TelTokenType = TelTokenType("TAP5_EL_INTEGER")

    @JvmField
    val TAP5_EL_DECIMAL: TelTokenType = TelTokenType("TAP5_EL_DECIMAL")

    @JvmField
    val TAP5_EL_BOOLEAN: TelTokenType = TelTokenType("TAP5_EL_BOOLEAN")

    @JvmField
    val TAP5_EL_NULL: TelTokenType = TelTokenType("TAP5_EL_NULL")

    @JvmField
    val TAP5_EL_BAD_CHAR: IElementType = TelTokenType("TAP5_EL_BAD_CHAR")

    @JvmField
    val TAP5_EL_CONTENT: IElementType = TelTokenType("TAP5_EL_CONTENT")

    @JvmField
    val TAP5_CONTEXT_NODE_KEY: Key<ASTNode> = Key.create("TAP5_CONTEXT_NODE_KEY")

    /** The lazily parsed `${...}` expression holder. */
    @JvmField
    val TAP5_EL_HOLDER: ILazyParseableElementType =
        object : ILazyParseableElementType("TAP5_EL_HOLDER", TelFileType.language) {

            override fun parseContents(chameleon: ASTNode): ASTNode? {
                val project = chameleon.psi.project
                val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon)
                val parser = LanguageParserDefinitions.INSTANCE.forLanguage(language).createParser(project)

                builder.putUserData(TAP5_CONTEXT_NODE_KEY, chameleon.treeParent)
                val result = parser.parse(this, builder).firstChildNode
                builder.putUserData(TAP5_CONTEXT_NODE_KEY, null)

                return result
            }
        }

    @JvmField
    val WHITESPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

    @JvmField
    val STRING_LITERALS: TokenSet = TokenSet.create(TAP5_EL_STRING)
}
