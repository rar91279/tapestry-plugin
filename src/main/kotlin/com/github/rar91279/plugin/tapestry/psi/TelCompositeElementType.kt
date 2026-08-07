package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.properties.references.PropertyReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiTypes
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IElementType
import com.github.rar91279.plugin.tapestry.lang.TelFileType

/**
 * A composite element type of the Tapestry Expression Language: knows how to build its PSI element.
 */
abstract class TelCompositeElementType(debugName: String) :
    IElementType(debugName, TelFileType.language), ICompositeElementType {

    abstract fun createPsiElement(node: ASTNode): PsiElement

    override fun createCompositeNode(): ASTNode = CompositeElement(this)

    companion object {

        /** A `prefix:value` binding; a `message:key` one references the message catalog. */
        val EXPLICIT_BINDING: TelCompositeElementType = object : TelCompositeElementType("ExplicitBinding") {

            override fun createPsiElement(node: ASTNode): PsiElement = object : TelCompositeElement(node) {

                override fun getReferences(): Array<PsiReference> {
                    var child = getNode().findChildByType(TelTokenTypes.TAP5_EL_IDENTIFIER)
                    if (child != null && child.text == "message") {
                        child = getNode().findChildByType(TelTokenTypes.TAP5_EL_IDENTIFIER, child.treeNext)
                        if (child != null) {
                            val psi = child.psi
                            val startOffsetInParent = psi.startOffsetInParent

                            return arrayOf(
                                PropertyReference(
                                    psi.text, this, null, true,
                                    TextRange(startOffsetInParent, startOffsetInParent + psi.textLength)
                                )
                            )
                        }
                    }

                    return super.getReferences()
                }
            }
        }

        val REFERENCE_EXPRESSION: TelCompositeElementType = object : TelCompositeElementType("ReferenceExpression") {
            override fun createPsiElement(node: ASTNode): PsiElement = TelReferenceExpression(node)
        }

        val ARGUMENT_LIST: TelCompositeElementType = object : TelCompositeElementType("ArgumentList") {
            override fun createPsiElement(node: ASTNode): PsiElement = TelArgumentList(node)
        }

        val METHOD_CALL_EXPRESSION: TelCompositeElementType = object : TelCompositeElementType("MethodCallExpression") {
            override fun createPsiElement(node: ASTNode): PsiElement = TelMethodCallExpression(node)
        }

        val RANGE_EXPRESSION: TelCompositeElementType = object : TelCompositeElementType("RangeExpression") {
            override fun createPsiElement(node: ASTNode): PsiElement = TelRangeExpression(node)
        }

        val NOT_OP_EXPRESSION: TelCompositeElementType = object : TelCompositeElementType("NotOpExpression") {
            override fun createPsiElement(node: ASTNode): PsiElement = TelNotOpExpression(node)
        }

        val LIST_EXPRESSION: TelCompositeElementType =
            TelLiteralExpressionType("ListExpression", CommonClassNames.JAVA_UTIL_LIST)

        val STRING_LITERAL: TelCompositeElementType =
            TelLiteralExpressionType("StringLiteral", CommonClassNames.JAVA_LANG_STRING)

        val INTEGER_LITERAL: TelCompositeElementType = TelLiteralExpressionType("IntegerLiteral", PsiTypes.intType())

        val DECIMAL_LITERAL: TelCompositeElementType = TelLiteralExpressionType("DoubleLiteral", PsiTypes.doubleType())

        val BOOLEAN_LITERAL: TelCompositeElementType = TelLiteralExpressionType("BooleanLiteral", PsiTypes.booleanType())

        val NULL_LITERAL: TelCompositeElementType = TelLiteralExpressionType("NullLiteral", PsiTypes.nullType())
    }
}
