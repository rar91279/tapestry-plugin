package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.CachedValuesManager
import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation

/** [IJavaAnnotation] backed by a PSI annotation. */
class IntellijJavaAnnotation(val psiAnnotation: PsiAnnotation) : IJavaAnnotation {

    override val fullyQualifiedName: String?
        get() = psiAnnotation.qualifiedName

    override val parameters: Map<String?, Array<String>>
        get() = CachedValuesManager.getProjectPsiDependentCache(psiAnnotation) { calcParameters(it) }

    private companion object {

        fun calcParameters(owner: PsiAnnotation): Map<String?, Array<String>> {
            val parameters = HashMap<String?, Array<String>>() // HashMap to handle null keys

            for (parameter in owner.parameterList.attributes) {
                val literalValue = parameter.literalValue
                if (literalValue != null) {
                    parameters[parameter.name] = arrayOf(literalValue)
                    continue
                }

                val value = parameter.value
                val stringValue = calcValue(value)

                if (stringValue != null) {
                    parameters[parameter.name] = arrayOf(stringValue)
                }
                else if (value is PsiArrayInitializerMemberValue) {
                    parameters[parameter.name] = value.initializers.map { calcValue(it).orEmpty() }.toTypedArray()
                }
            }

            return parameters
        }

        fun calcValue(value: PsiAnnotationMemberValue?): String? {
            if (value is PsiLiteralExpression) return value.value?.toString()

            if (value is PsiReferenceExpression) {
                val initializer = (value.resolve() as? PsiField)?.initializer
                if (initializer is PsiLiteralExpression) return initializer.value?.toString()
            }

            return null
        }
    }
}
