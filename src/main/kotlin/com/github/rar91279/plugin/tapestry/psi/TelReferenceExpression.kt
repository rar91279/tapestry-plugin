package com.github.rar91279.plugin.tapestry.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.util.PropertyUtilBase

/** A property or method reference, optionally qualified by another expression. */
class TelReferenceExpression(node: ASTNode) : TelCompositeElement(node), TelReferenceQualifier {

    private val myReference: TelQualifiedReference = object : TelQualifiedReference(this@TelReferenceExpression) {

        override fun getRangeInElement(): TextRange {
            val element = referenceNameElement ?: return TextRange.from(0, textLength)

            return TextRange.from(element.startOffsetInParent, element.textLength)
        }

        override fun getReferenceName(): String? = referenceNameElement?.text

        override fun getReferenceQualifier(): TelReferenceQualifier? =
            findChildByClass(TelReferenceQualifier::class.java)

        override fun handleElementRename(newElementName: String): PsiElement {
            var newName = newElementName

            // if we referenced property name before (without get) then rename should also strip get prefix
            val resolved = resolve()
            if (resolved is PsiMethod && PropertyUtilBase.getPropertyName(resolved) != null) {
                PropertyUtilBase.getPropertyName(newName)?.let { newName = it }
            }

            val newReferenceName = TelPsiUtil.parseReference(newName, project).referenceNameElement
            val oldReferenceName = referenceNameElement
            if (newReferenceName != null && oldReferenceName != null) {
                node.replaceChild(oldReferenceName.node, newReferenceName.node)
            }

            return this@TelReferenceExpression
        }
    }

    internal val referenceNameElement: PsiElement?
        get() = findChildByType(TelTokenTypes.TAP5_EL_IDENTIFIER)

    override fun getReference(): TelQualifiedReference = myReference

    override fun getPsiType(): PsiType? = myReference.getPsiType()
}
