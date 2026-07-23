package com.intellij.tapestry.psi;

import com.intellij.psi.tree.IFileElementType;
import com.intellij.tapestry.lang.TmlLanguage;

/**
 * @author Alexey Chmutov
 */
public interface TmlElementType {
  IFileElementType TML_FILE = new IFileElementType("TML_FILE", TmlLanguage.INSTANCE);
}
