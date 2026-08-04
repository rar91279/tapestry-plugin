package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lang.xml.XMLLanguage
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Parser definition of a Tapestry template: XML parsing with a Tapestry-EL aware lexer.
 */
class TmlParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = TmlLexer()

    override fun getFileNodeType(): IFileElementType = TmlElementType.TML_FILE

    override fun getWhitespaceTokens(): TokenSet = xmlParserDefinition().whitespaceTokens

    override fun getCommentTokens(): TokenSet = xmlParserDefinition().commentTokens

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createParser(project: Project?): PsiParser = xmlParserDefinition().createParser(project)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = TmlFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements =
        xmlParserDefinition().spaceExistenceTypeBetweenTokens(left, right)

    override fun createElement(node: ASTNode): PsiElement = throw IllegalArgumentException("Unknown element: $node")

    private fun xmlParserDefinition(): ParserDefinition =
        LanguageParserDefinitions.INSTANCE.forLanguage(Language.findInstance(XMLLanguage::class.java))
}
