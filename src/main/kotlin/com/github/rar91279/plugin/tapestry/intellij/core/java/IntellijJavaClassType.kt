package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiUtil
import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod
import com.github.rar91279.plugin.tapestry.core.resource.IResource
import com.github.rar91279.plugin.tapestry.intellij.core.resource.IntellijResource
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils

/** [IJavaClassType] backed by the public class of a PSI file. */
class IntellijJavaClassType(private val module: Module, psiFile: PsiFile) : IntellijJavaType(), IJavaClassType {

    private val classFilePath: String = (psiFile.virtualFile ?: psiFile.viewProvider.virtualFile).url
    private var psiClassType: PsiClassType? = null
    private var supportInformalParameters: Boolean? = null

    override val fullyQualifiedName: String?
        get() = psiClass?.qualifiedName

    override val name: String?
        get() = psiClass?.name

    override val isInterface: Boolean
        get() = psiClass?.isInterface == true

    override val isPublic: Boolean
        get() = psiClass?.modifierList?.hasModifierProperty(PsiModifier.PUBLIC) == true

    override val isEnum: Boolean
        get() = psiClass?.isEnum == true

    override val superClassType: IntellijJavaClassType?
        get() = psiClass?.superClass?.containingFile?.let { IntellijJavaClassType(module, it) }

    override fun hasDefaultConstructor(): Boolean {
        val psiClass = psiClass ?: return false
        return PsiUtil.hasDefaultConstructor(psiClass)
    }

    override fun getPublicMethods(fromSuper: Boolean): Collection<IJavaMethod> =
        methods(fromSuper)
            .filter { it.modifierList.hasExplicitModifier(PsiModifier.PUBLIC) && it.isNotMethodOfJavaLangObject() }
            .map { IntellijJavaMethod(module, it) }

    override fun getAllMethods(fromSuper: Boolean): Collection<IJavaMethod> =
        methods(fromSuper)
            .filter { it.isNotMethodOfJavaLangObject() }
            .map { IntellijJavaMethod(module, it) }

    override fun findPublicMethods(methodNameRegExp: String): Collection<IJavaMethod> {
        val nameRegex = Regex(methodNameRegExp)

        return getPublicMethods(true).filter { nameRegex.matches(it.name.orEmpty()) }
    }

    override val annotations: Collection<IJavaAnnotation>
        get() = psiClass?.modifierList?.annotations?.map { IntellijJavaAnnotation(it) } ?: emptyList()

    override fun getFields(fromSuper: Boolean): Map<String, IJavaField> {
        val psiClass = psiClass ?: return emptyMap()

        val classFields = try {
            if (fromSuper) psiClass.allFields else psiClass.fields
        }
        catch (ex: PsiInvalidElementAccessException) {
            // thrown if the class is invalid, should ignore and return an empty Map
            return emptyMap()
        }

        return classFields.associate { it.name to IntellijJavaField(module, it) }
    }

    override val documentation: String
        get() = psiClass?.javadocDescription() ?: ""

    override val file: IResource?
        get() {
            val virtualFile = VirtualFileManager.getInstance().findFileByUrl(classFilePath) ?: return null

            return PsiManager.getInstance(module.project).findFile(virtualFile)?.let { IntellijResource(it) }
        }

    override fun supportsInformalParameters(): Boolean {
        supportInformalParameters?.let { return it }

        val psiClass = psiClass
        val result = psiClass != null &&
                     AnnotationUtil.isAnnotated(psiClass, INFORMAL_PARAMETERS_ANNOTATION, AnnotationUtil.CHECK_HIERARCHY)

        return result.also { supportInformalParameters = it }
    }

    override val underlyingObject: Any?
        get() {
            if (psiClassType == null) processPsiClassType()
            return psiClassType
        }

    /** The psi class associated with this class. */
    val psiClass: PsiClass?
        get() {
            val current = psiClassType
            if (current != null && current.isValid && current.resolve()?.containingFile?.isValid == true) {
                return current.resolve()
            }

            processPsiClassType()
            return psiClassType?.resolve()
        }

    private fun methods(fromSuper: Boolean): List<PsiMethod> {
        val psiClass = psiClass ?: return emptyList()
        return (if (fromSuper) psiClass.allMethods else psiClass.methods).toList()
    }

    private fun PsiMethod.isNotMethodOfJavaLangObject(): Boolean =
        containingClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT

    private fun processPsiClassType() {
        val file = VirtualFileManager.getInstance().findFileByUrl(classFilePath)
        if (file == null) {
            psiClassType = null
            return
        }

        val psiFile = PsiManager.getInstance(module.project).findFile(file)
        if (psiFile !is PsiClassOwner) {
            // Not a class-bearing file (e.g. a .tml resource). psiClass is nullable and all
            // callers guard for null, so degrade gracefully instead of throwing.
            psiClassType = null
            return
        }

        val psiClasses = psiFile.classes
        val aClass = IdeaUtils.findPublicClass(psiClasses) ?: psiClasses.firstOrNull()

        psiClassType = aClass?.let {
            JavaPsiFacade.getInstance(module.project).elementFactory.createType(it)
        }
    }

    private companion object {
        const val INFORMAL_PARAMETERS_ANNOTATION = "org.apache.tapestry5.annotations.SupportsInformalParameters"
    }
}
