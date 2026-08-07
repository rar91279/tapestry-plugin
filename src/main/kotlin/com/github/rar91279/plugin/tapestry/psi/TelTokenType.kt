package com.github.rar91279.plugin.tapestry.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.github.rar91279.plugin.tapestry.lang.TelFileType

/** A token of the Tapestry Expression Language. */
class TelTokenType(debugName: String) : IElementType(debugName, TelFileType.language) {

    fun createPsiElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
}
