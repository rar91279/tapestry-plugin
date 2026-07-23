package com.intellij.tapestry.tests;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.tapestry.core.TapestryConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Alexey Chmutov
 */
public final class Util {
  static final String DOT_TML = "." + TapestryConstants.TEMPLATE_FILE_EXTENSION;
  static final String DOT_JAVA = ".java";
  static final String DOT_GROOVY = ".groovy";
  static final String AFTER = "_after";
  public static final String DOT_EXPECTED = ".expected";

  private Util() {
  }

  static String getFileText(final String filePath) {
    try {
      return FileUtil.loadFile(new File(filePath));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  static String getCommonTestDataFileText(@NotNull String fileName) {
    return getFileText(getCommonTestDataPath() + "/" + fileName);
  }

  static String getCommonTestDataPath() {
    // Standalone repo layout: test data lives in the module under src/test/testData.
    // The test working directory is the tapestry module dir (same convention the module fixtures use).
    return new File("").getAbsoluteFile().getPath().replace(File.separatorChar, '/') + "/src/test/testData/";
  }
}
