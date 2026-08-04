package com.github.rar91279.plugin.tapestry.intellij.core.java

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.util.ClassUtil
import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.java.IJavaTypeCreator
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.intellij.util.IncorrectOperationException
import com.siyeh.ig.psiutils.ImportUtils

/** [IJavaTypeCreator] that builds PSI elements for a module. */
open class IntellijJavaTypeCreator(private val module: Module) : IJavaTypeCreator {

    override fun createField(
        name: String,
        type: IJavaClassType,
        isPrivate: Boolean,
        changeNameToReflectIdeSettings: Boolean
    ): IJavaField? {
        val fieldName =
            if (changeNameToReflectIdeSettings) {
                JavaCodeStyleManager.getInstance(module.project)
                    .propertyNameToVariableName(StringUtil.decapitalize(name), VariableKind.FIELD)
            }
            else name

        return try {
            val elementFactory = JavaPsiFacade.getInstance(module.project).elementFactory
            val field = elementFactory.createField(
                fieldName, elementFactory.createType((type as IntellijJavaClassType).psiClass!!)
            )

            field.modifierList?.setModifierProperty(PsiModifier.PRIVATE, isPrivate)

            IntellijJavaField(module, field)
        }
        catch (ex: Throwable) {
            logger.error(ex)
            null
        }
    }

    override fun createFieldAnnotation(
        field: IJavaField,
        fullyQualifiedName: String,
        parameters: Map<String, String>
    ): IJavaAnnotation? {
        val annotationText = StringBuilder("@").append(fullyQualifiedName)
        if (parameters.isNotEmpty()) {
            annotationText.append(
                parameters.entries.joinToString(",", prefix = "(", postfix = ")") { (name, value) ->
                    // a value that is already a { ... } array initializer is inlined verbatim
                    if (value.startsWith("{")) "$name=$value" else "$name=\"$value\""
                }
            )
        }

        val psiField = (field as IntellijJavaField).psiField

        return try {
            val annotation: PsiAnnotation = JavaPsiFacade.getInstance(module.project).elementFactory
                .createAnnotationFromText(annotationText.toString(), psiField)
            psiField.modifierList?.addBefore(annotation, psiField.modifierList?.firstChild)

            CodeStyleManager.getInstance(module.project).reformat(psiField)

            IntellijJavaAnnotation(annotation)
        }
        catch (ex: IncorrectOperationException) {
            logger.error(ex)
            null
        }
    }

    override fun ensureClassImport(baseClass: IJavaClassType, type: IJavaClassType): Boolean {
        val baseFile = (baseClass as IntellijJavaClassType).psiClass?.containingFile ?: return false
        val typeFqn = type.fullyQualifiedName ?: return false

        if (!ImportUtils.nameCanBeImported(typeFqn, baseFile)) return false

        val importList = (baseFile as? PsiJavaFile)?.importList ?: return false

        if (ClassUtil.extractPackageName(typeFqn) == "java.lang") {
            // JavaCodeStyleManager.addImport internally decides whether a java.lang class actually
            // needs an explicit import (i.e. only on an on-demand import conflict).
            IdeaUtils.runWriteCommand(null) {
                (type as IntellijJavaClassType).psiClass?.let {
                    JavaCodeStyleManager.getInstance(module.project).addImport(baseFile, it)
                }
            }
            return true
        }

        if (importList.findSingleClassImportStatement(typeFqn) == null) {
            IdeaUtils.runWriteCommand(null) {
                try {
                    (type as IntellijJavaClassType).psiClass?.let { addImport(importList, it) }
                }
                catch (ex: IncorrectOperationException) {
                    logger.error(ex)
                }
            }

            val fileEditorManager = FileEditorManager.getInstance(module.project)
            if (fileEditorManager.selectedFiles.isNotEmpty()) {
                fileEditorManager.selectedTextEditor?.document?.let {
                    PsiDocumentManager.getInstance(module.project).doPostponedOperationsAndUnblockDocument(it)
                }
            }
        }

        return true
    }

    /**
     * Adds an import statement for the given class.
     *
     * @throws IncorrectOperationException if an error occurs.
     */
    @Throws(IncorrectOperationException::class)
    open fun addImport(importList: PsiImportList, aClass: PsiClass) {
        val elementFactory = JavaPsiFacade.getInstance(importList.project).elementFactory
        importList.add(elementFactory.createImportStatement(aClass))
    }

    private companion object {
        val logger = Logger.getInstance(IntellijJavaTypeCreator::class.java)
    }
}
