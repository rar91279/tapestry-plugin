package com.intellij.tapestry.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.util.PlatformIcons
import org.jetbrains.annotations.NonNls

/**
 * File type for Tapestry Expression Language (TEL) files.
 *
 * TEL is a language used in Apache Tapestry 5 framework for writing expressions
 * in templates and components.
 *
 * @author Alexey Chmutov
 */
object TelFileType: LanguageFileType(TelLanguage.INSTANCE) {
    /**
     * Returns the name of this file type.
     *
     * @return the file type name "TEL"
     */
    @NonNls
    override fun getName() =  "TEL"

    /**
     * Returns a human-readable description of this file type.
     *
     * @return the description of TEL file type
     */
    override fun getDescription() = "Expression Language in Tapestry 5"

    /**
     * Returns the default file extension for this file type.
     *
     * @return the default extension "tel"
     */
    @NonNls
    override fun getDefaultExtension() = "tel"

    /**
     * Returns the icon associated with this file type.
     *
     * @return the custom file icon from platform icons
     */
    override fun getIcon()=  PlatformIcons.CUSTOM_FILE_ICON
}
