package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMethod
import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.core.java.IMethodParameter
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils

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
