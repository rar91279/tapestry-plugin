package com.github.rar91279.plugin.tapestry.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTagChild
import com.intellij.xml.util.XmlUtil

/** Holds a `${...}` expression inside a template tag. */
class TelExpressionHolder(node: ASTNode) : ASTWrapperPsiElement(node), XmlTagChild {

    override fun getParentTag(): XmlTag? = parent as? XmlTag

    override fun getNextSiblingInTag(): XmlTagChild? = nextSibling as? XmlTagChild

    override fun getPrevSiblingInTag(): XmlTagChild? = prevSibling as? XmlTagChild

    override fun processElements(processor: PsiElementProcessor<in PsiElement>, place: PsiElement?): Boolean =
        XmlUtil.processXmlElements(this, processor, false)

    override fun toString(): String = "TelExpressionHolder"
}
