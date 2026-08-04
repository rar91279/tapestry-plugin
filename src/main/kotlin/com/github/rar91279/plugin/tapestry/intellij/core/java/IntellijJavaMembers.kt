package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiWhiteSpace
import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.java.IMethodParameter
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

/** [IJavaMethod] backed by a PSI method. */
class IntellijJavaMethod(private val module: Module, val psiMethod: PsiMethod) : IJavaMethod {

    override val name: String
        get() = psiMethod.name

    override val returnType: IJavaType?
        get() = IdeaUtils.createJavaTypeFromPsiType(module, psiMethod.returnType)

    override val parameters: Collection<IMethodParameter>
        get() = psiMethod.parameterList.parameters.map { IntellijMethodParameter(module, it) }

    override val annotations: Collection<IJavaAnnotation>
        get() = psiMethod.modifierList.annotations.map { IntellijJavaAnnotation(it) }

    override fun getAnnotation(annotationQualifiedName: String?): IJavaAnnotation? =
        annotations.firstOrNull { it.fullyQualifiedName == annotationQualifiedName }

    override val containingClass: IJavaClassType?
        get() = psiMethod.containingClass?.containingFile?.let { IntellijJavaClassType(module, it) }

    override val documentation: String
        get() = psiMethod.javadocDescription()
}

/**
 * The javadoc description of this element, empty when it has none. Falls back to the navigation
 * element, whose language may be one without javadoc (e.g. a Kotlin `KtProperty`).
 */
internal fun PsiDocCommentOwner.javadocDescription(): String {
    val docComment = docComment ?: (navigationElement as? PsiDocCommentOwner)?.docComment ?: return ""

    return docComment.descriptionElements
        .filter { it !is PsiWhiteSpace }
        .joinToString("") { it.text }
}
