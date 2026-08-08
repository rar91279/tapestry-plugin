package com.github.rar91279.plugin.tapestry.intellij.util

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.util.ClassUtil
import com.intellij.util.IncorrectOperationException
import com.siyeh.ig.psiutils.ImportUtils

/** Builds the PSI elements the "add new element" actions and the class externalizer write out. */
open class JavaTypeCreator(private val module: Module) {

    /**
     * Creates a new field.
     *
     * @param changeNameToReflectIdeSettings `true` if the IDE coding style should be used to change the field name accordingly.
     */
    fun createField(
        name: String,
        type: PsiClass,
        isPrivate: Boolean,
        changeNameToReflectIdeSettings: Boolean
    ): PsiField? {
        val fieldName =
            if (changeNameToReflectIdeSettings) {
                JavaCodeStyleManager.getInstance(module.project)
                    .propertyNameToVariableName(StringUtil.decapitalize(name), VariableKind.FIELD)
            }
            else name

        return try {
            val elementFactory = JavaPsiFacade.getInstance(module.project).elementFactory
            val field = elementFactory.createField(fieldName, elementFactory.createType(type))

            field.modifierList?.setModifierProperty(PsiModifier.PRIVATE, isPrivate)

            field
        }
        catch (ex: Throwable) {
            // Throwable, so this also caught ControlFlowException and ProcessCanceledException.
            if (ex is ControlFlowException) throw ex
            logger.error(ex)
            null
        }
    }

    /** Creates a new field annotation and adds it to the field. */
    fun createFieldAnnotation(
        field: PsiField,
        fullyQualifiedName: String,
        parameters: Map<String, String>
    ): PsiAnnotation? {
        val annotationText = StringBuilder("@").append(fullyQualifiedName)
        if (parameters.isNotEmpty()) {
            annotationText.append(
                parameters.entries.joinToString(",", prefix = "(", postfix = ")") { (name, value) ->
                    // a value that is already a { ... } array initializer is inlined verbatim
                    if (value.startsWith("{")) "$name=$value" else "$name=\"$value\""
                }
            )
        }

        return try {
            val annotation: PsiAnnotation = JavaPsiFacade.getInstance(module.project).elementFactory
                .createAnnotationFromText(annotationText.toString(), field)
            field.modifierList?.addBefore(annotation, field.modifierList?.firstChild)

            CodeStyleManager.getInstance(module.project).reformat(field)

            annotation
        }
        catch (ex: IncorrectOperationException) {
            logger.error(ex)
            null
        }
    }

    /**
     * Ensures that a type is in the import list of a class.
     *
     * @return `true` if the import was insured, `false` otherwise.
     */
    fun ensureClassImport(baseClass: PsiClass, type: PsiClass): Boolean {
        val baseFile = baseClass.containingFile ?: return false
        val typeFqn = type.qualifiedName ?: return false

        if (!ImportUtils.nameCanBeImported(typeFqn, baseFile)) return false

        val importList = (baseFile as? PsiJavaFile)?.importList ?: return false

        if (ClassUtil.extractPackageName(typeFqn) == "java.lang") {
            // JavaCodeStyleManager.addImport internally decides whether a java.lang class actually
            // needs an explicit import (i.e. only on an on-demand import conflict).
            IdeaUtils.runWriteCommand(null) {
                JavaCodeStyleManager.getInstance(module.project).addImport(baseFile, type)
            }
            return true
        }

        if (importList.findSingleClassImportStatement(typeFqn) == null) {
            IdeaUtils.runWriteCommand(null) {
                try {
                    addImport(importList, type)
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
        val logger = Logger.getInstance(JavaTypeCreator::class.java)
    }
}
