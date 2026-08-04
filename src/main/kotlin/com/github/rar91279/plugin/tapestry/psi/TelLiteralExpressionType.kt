package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType

/**
 * The element type of a TEL literal, of either a class type (by name) or a primitive type.
 */
class TelLiteralExpressionType private constructor(
    debugName: String,
    private val typeName: String?,
    private val primitiveType: PsiPrimitiveType?
) : TelCompositeElementType(debugName) {

    constructor(debugName: String, typeName: String) : this(debugName, typeName, null)

    constructor(debugName: String, primitiveType: PsiType) :
        this(debugName, null, primitiveType as PsiPrimitiveType)

    override fun createPsiElement(node: ASTNode): PsiElement = TelLiteralExpression(node)

    inner class TelLiteralExpression(node: ASTNode) : TelCompositeElement(node), TelExpression {

        override fun getPsiType(): PsiType? = primitiveType
            ?: JavaPsiFacade.getInstance(project).elementFactory
                .createTypeByFQClassName(typeName!!, resolveScope)
    }
}
