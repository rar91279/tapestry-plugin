package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils

/** [IJavaField] backed by a PSI field. */
class IntellijJavaField(private val module: Module, val psiField: PsiField) : IJavaField {

    override val name: String
        get() = psiField.name

    override val type: IJavaType?
        get() = IdeaUtils.createJavaTypeFromPsiType(module, psiField.type)

    override val isPrivate: Boolean
        get() = psiField.modifierList?.hasModifierProperty(PsiModifier.PRIVATE) == true

    override val annotations: Map<String, IJavaAnnotation>
        get() = psiField.modifierList?.annotations
            ?.filter { it.isValid }
            ?.mapNotNull { annotation -> annotation.qualifiedName?.let { it to IntellijJavaAnnotation(annotation) } }
            ?.toMap()
            ?: emptyMap()

    override val documentation: String
        get() = psiField.javadocDescription()

    override val stringRepresentation: String
        get() = psiField.text

    override val isValid: Boolean
        get() = psiField.isValid

    override fun equals(other: Any?): Boolean = other is IntellijJavaField && name == other.name

    override fun hashCode(): Int = name.hashCode()
}
