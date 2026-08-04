package com.github.rar91279.plugin.tapestry.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageUtil
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.github.rar91279.plugin.tapestry.lang.TelFileType
import com.github.rar91279.plugin.tapestry.psi.impl.TelExpressionHolder

/**
 * Parser definition of the Tapestry Expression Language.
 */
class TelParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = TelLexer()

    override fun getFileNodeType(): IFileElementType = TelTokenTypes.TEL_FILE

    override fun getWhitespaceTokens(): TokenSet = TelTokenTypes.WHITESPACES

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TelTokenTypes.STRING_LITERALS

    override fun createParser(project: Project?): PsiParser = TelParser()

    override fun createFile(viewProvider: FileViewProvider): PsiFile = TelFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements =
        LanguageUtil.canStickTokensTogetherByLexer(left, right, createLexer(left.psi.project))

    override fun createElement(node: ASTNode): PsiElement {
        val elementType = node.elementType

        if (elementType is TelCompositeElementType) return elementType.createPsiElement(node)
        if (elementType === TelTokenTypes.TAP5_EL_HOLDER) return TelExpressionHolder(node)

        throw AssertionError("Unknown type: $elementType")
    }

    /** A standalone TEL file, as parsed from a snippet. */
    private class TelFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TelFileType.language) {

        init {
            init(TelTokenTypes.TEL_FILE, TelTokenTypes.TAP5_EL_HOLDER)
        }

        override fun getFileType(): FileType = TelFileType
    }
}
