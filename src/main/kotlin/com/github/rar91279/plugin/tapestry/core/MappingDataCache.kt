package com.github.rar91279.plugin.tapestry.core

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaReference
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.annotations.VisibleForTesting

/**
 * Caches the library prefix to package mappings declared with `new LibraryMapping(...)`.
 */
@VisibleForTesting
object MappingDataCache {

    private const val TAPESTRY_MAPPING_FQN = "org.apache.tapestry5.services.LibraryMapping"

    @JvmStatic
    fun getMappingData(file: PsiFile): Map<String, String> =
        CachedValuesManager.getProjectPsiDependentCache(file, ::computeMappingData)

    private fun computeMappingData(file: PsiFile): Map<String, String> {
        val result = HashMap<String, String>()

        file.sourceFile().accept(object : JavaRecursiveElementVisitor() {
            override fun visitNewExpression(expression: PsiNewExpression) {
                if (expression.classReference?.qualifiedName == TAPESTRY_MAPPING_FQN) {
                    val expressions = expression.argumentList?.expressions
                    if (expressions != null && expressions.size == 2) {
                        val prefix = expressions[0].constantStringValue()
                        val packageName = expressions[1].constantStringValue()

                        if (prefix != null && packageName != null) {
                            result[prefix] = packageName
                        }
                    }
                }
                super.visitNewExpression(expression)
            }
        })

        return result
    }

    /** The source file behind a compiled one, or the file itself. */
    private fun PsiFile.sourceFile(): PsiFile {
        if (this !is PsiCompiledElement) return this

        val navigationElement = navigationElement
        if (navigationElement !== this && navigationElement is PsiFile) return navigationElement

        return mirror as? PsiFile ?: this
    }

    private fun PsiExpression.constantStringValue(): String? = when (this) {
        is PsiJavaReference -> {
            val field = resolve() as? PsiField
            if (field != null && field.hasModifierProperty(PsiModifier.FINAL) && field.hasInitializer()) {
                field.computeConstantValue() as? String
            }
            else null
        }

        is PsiLiteralExpression -> StringUtil.unquoteString(text)

        else -> null
    }
}
