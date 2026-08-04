package com.github.rar91279.plugin.tapestry.lang

import com.intellij.ide.highlighter.XmlLikeFileType
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import icons.TapestryIcons

/**
 * File type implementation for Tapestry Markup Language (TML) files.
 *
 * This file type represents template files used in the Apache Tapestry web framework.
 * TML files are XML-like templates that define the structure and layout of Tapestry
 * components and pages. This implementation provides IDE integration for TML files,
 * including syntax highlighting, file recognition, and visual representation.
 *
 * The file type is associated with the [TmlLanguage] language instance and extends
 * [XmlLikeFileType] to inherit XML-related functionality while providing Tapestry-specific
 * features.
 *
 * ## File Characteristics
 * - **File Extension**: `.tml` (defined in [TapestryConstants.TEMPLATE_FILE_EXTENSION])
 * - **Language Name**: "TML" (defined in [TapestryConstants.EL_LANGUAGE])
 * - **Description**: "Tapestry template"
 * - **Icon**: Tapestry logo (16x18 pixels)
 *
 * ## Usage
 * This file type is automatically registered with the IntelliJ Platform and is used
 * to identify and process TML files throughout the IDE. It provides:
 * - File type detection based on extension
 * - Visual identification through the Tapestry icon
 * - Language-specific features through [TmlLanguage]
 * - XML-like syntax support through parent class
 *
 * @see TmlLanguage
 * @see XmlLikeFileType
 * @see TapestryConstants
 */
object TmlFileType: XmlLikeFileType(TmlLanguage) {

    override fun getName() = TapestryConstants.EL_LANGUAGE

    override fun getDescription() = "Tapestry template"

    override fun getDefaultExtension() =  TapestryConstants.TEMPLATE_FILE_EXTENSION

    override fun getIcon() = TapestryIcons.Tapestry_logo_small
}
