package com.intellij.tapestry.lang;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class TmlTypedHandler extends TypedHandlerDelegate {
  @NotNull
  @Override
  public Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (file.getFileType() != TmlFileType.INSTANCE || c != '{') return Result.CONTINUE;
    if(!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return Result.CONTINUE;
    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
    final int offset = editor.getCaretModel().getOffset();
    final PsiElement elementAt = file.findElementAt(offset - 1);

    if (elementAt != null) {
      final int index = offset - 2;

      if (index >= 0) {
        final CharSequence charSequence = editor.getDocument().getCharsSequence();

        if (charSequence.length() > index) {
          // Auto-insert the closing brace for a Tapestry EL start "${", unless one already follows the
          // caret. (The previous TAP5_EL_HOLDER PSI check is unreliable on 2026.2, where an incomplete
          // "${" already parses as a holder.)
          if (charSequence.charAt(index) == '$'
              && (offset >= charSequence.length() || charSequence.charAt(offset) != '}')) {
            editor.getDocument().insertString(offset, "}");
            return Result.STOP;
          }
        }
      }
    }

    return Result.CONTINUE;
  }
}
