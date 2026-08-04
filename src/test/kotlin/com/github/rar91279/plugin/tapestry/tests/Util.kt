package com.github.rar91279.plugin.tapestry.tests

import com.intellij.openapi.util.io.FileUtil
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import java.io.File
import java.io.IOException

/**
 * @author Alexey Chmutov
 */
object Util {
    val DOT_TML: String = "." + TapestryConstants.TEMPLATE_FILE_EXTENSION
    const val DOT_JAVA: String = ".java"
    const val DOT_GROOVY: String = ".groovy"
    const val DOT_KOTLIN: String = ".kt"
    const val AFTER: String = "_after"
    const val DOT_EXPECTED: String = ".expected"

    fun getFileText(filePath: String): String {
        try {
            return FileUtil.loadFile(File(filePath))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun getCommonTestDataFileText(fileName: String): String {
        return getFileText(getCommonTestDataPath() + "/" + fileName)
    }

    fun getCommonTestDataPath(): String {
        // Standalone repo layout: test data lives in the module under src/test/testData.
        // The test working directory is the tapestry module dir (same convention the module fixtures use).
        return File("").absoluteFile.path.replace(File.separatorChar, '/') + "/src/test/testData/"
    }
}
