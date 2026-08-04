package com.github.rar91279.plugin.tapestry.lang

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class TmlTypedHandler : TypedHandlerDelegate() {
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        when {
            file.fileType !== TmlFileType || c != '{' -> return Result.CONTINUE

            !CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET -> return Result.CONTINUE

            else -> {
                PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                val offset = editor.caretModel.offset
                val elementAt = file.findElementAt(offset - 1)

                if (elementAt != null) {
                    val index = offset - 2

                    if (index >= 0) {
                        val charSequence = editor.document.charsSequence

                        if (charSequence.length > index) {
                            // Auto-insert the closing brace for a Tapestry EL start "${", unless one already follows the
                            // caret. (The previous TAP5_EL_HOLDER PSI check is unreliable on 2026.2, where an incomplete
                            // "${" already parses as a holder.
                            if (charSequence[index] == '$'
                                && (offset >= charSequence.length || charSequence[offset] != '}')
                            ) {
                                editor.document.insertString(offset, "}")
                                return Result.STOP
                            }
                        }
                    }
                }
                return Result.CONTINUE
            }
        }
    }
}
