package com.github.rar91279.plugin.tapestry.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.PropertyUtilBase

/** An expression of the Tapestry Expression Language. */
interface TelExpression : PsiElement {

    fun getPsiType(): PsiType?
}

/** An expression that can qualify a property reference. */
interface TelReferenceQualifier : TelExpression

/** Base class of the composite TEL elements. */
open class TelCompositeElement(node: ASTNode) : ASTWrapperPsiElement(node) {

    override fun toString(): String = node.elementType.toString()
}

/** The argument list of a method call. */
class TelArgumentList(node: ASTNode) : TelCompositeElement(node) {

    val arguments: Array<TelExpression>
        get() = findChildrenByClass(TelExpression::class.java)
}

/** A `!expression` negation. */
class TelNotOpExpression(node: ASTNode) : TelCompositeElement(node), TelExpression {

    override fun getPsiType(): PsiType = PsiTypes.booleanType()
}

/** A `from..to` range. */
class TelRangeExpression(node: ASTNode) : TelCompositeElement(node), TelExpression {

    override fun getPsiType(): PsiType? = JavaPsiFacade.getInstance(project).elementFactory
        .createTypeByFQClassName(CommonClassNames.JAVA_UTIL_LIST, resolveScope)
}

/** A `method(args)` call. */
class TelMethodCallExpression(node: ASTNode) : TelCompositeElement(node), TelReferenceQualifier {

    val argumentTypes: Array<PsiType?>
        get() = argumentList.arguments.map { it.getPsiType() }.toTypedArray()

    val argumentList: TelArgumentList
        get() = findNotNullChildByClass(TelArgumentList::class.java)

    override fun getPsiType(): PsiType? = findNotNullChildByClass(TelReferenceExpression::class.java).getPsiType()
}

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
