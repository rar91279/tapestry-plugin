package com.github.rar91279.plugin.tapestry.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiType
import com.github.rar91279.plugin.tapestry.lang.TelFileType

/**
 * Parses standalone TEL snippets and renders TEL types.
 */
object TelPsiUtil {

    private const val NULL_TYPE_NAME = "???"

    @JvmStatic
    fun parseReference(text: String, project: Project): TelReferenceExpression {
        val expression = parseFtlFile("\${$text}", project)
        val interpolation = expression.firstChild
        val elStart = interpolation.firstChild

        return elStart.nextSibling as TelReferenceExpression
    }

    @JvmStatic
    fun parseFtlFile(text: String, project: Project): PsiElement =
        PsiFileFactory.getInstance(project).createFileFromText("dummy.tel", TelFileType, text).firstChild

    @JvmStatic
    fun getPresentableText(psiType: PsiType?): String = psiType?.presentableText ?: NULL_TYPE_NAME
}
