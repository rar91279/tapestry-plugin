package com.github.rar91279.plugin.tapestry.intellij.lang

import com.intellij.ide.highlighter.JavaHighlightingColors
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.JspHighlighterColors
import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.util.io.FileUtil
import com.github.rar91279.plugin.tapestry.lang.TmlHighlighter
import icons.TapestryIcons
import java.io.IOException
import javax.swing.Icon

/**
 * Tapestry template color settings.
 */
class TemplateColorSettingsPage : ColorSettingsPage {

    override fun getDisplayName(): String = "Tapestry"

    override fun getIcon(): Icon = TapestryIcons.Tapestry_logo_small

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = ATTRIBUTE_DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getHighlighter(): SyntaxHighlighter = TmlHighlighter()

    override fun getDemoText(): String = demoText

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "componenTagName" to TAG_NAME,
        "componenTagAttribute" to ATTR_NAME
    )

    companion object {

        @JvmField
        val TAG_NAME: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TAPESTRY_COMPONENT_TAG", XmlHighlighterColors.HTML_TAG_NAME)

        @JvmField
        val ATTR_NAME: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TAPESTRY_COMPONENT_PARAMATER", XmlHighlighterColors.HTML_ATTRIBUTE_NAME)

        @JvmField
        val TEL_BOUNDS: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_BOUNDS", JavaHighlightingColors.KEYWORD)

        @JvmField
        val TEL_IDENT: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_IDENT", JavaHighlightingColors.LOCAL_VARIABLE_ATTRIBUTES)

        @JvmField
        val TEL_DOT: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_DOT", JavaHighlightingColors.DOT)

        @JvmField
        val TEL_NUMBER: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_NUMBER", JavaHighlightingColors.NUMBER)

        @JvmField
        val TEL_PARENTHS: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_PARENTHS", JavaHighlightingColors.PARENTHESES)

        @JvmField
        val TEL_BRACKETS: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_BRACKETS", JavaHighlightingColors.BRACKETS)

        @JvmField
        val TEL_STRING: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_STRING", JavaHighlightingColors.STRING)

        @JvmField
        val TEL_BACKGROUND: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_BACKGROUND", JspHighlighterColors.JSP_SCRIPTING_BACKGROUND)

        @JvmField
        val TEL_BAD_CHAR: TextAttributesKey =
            TextAttributesKey.createTextAttributesKey("TEL_BAD_CHAR", HighlighterColors.BAD_CHARACTER)

        private val ATTRIBUTE_DESCRIPTORS = arrayOf(
            AttributesDescriptor("Component tag", TAG_NAME),
            AttributesDescriptor("Component parameter", ATTR_NAME),
            AttributesDescriptor("EL bounds", TEL_BOUNDS),
            AttributesDescriptor("EL identifier", TEL_IDENT),
            AttributesDescriptor("EL dot", TEL_DOT),
            AttributesDescriptor("EL number", TEL_NUMBER),
            AttributesDescriptor("EL parenths", TEL_PARENTHS),
            AttributesDescriptor("EL brackets", TEL_BRACKETS),
            AttributesDescriptor("EL string", TEL_STRING),
            AttributesDescriptor("EL background", TEL_BACKGROUND),
            AttributesDescriptor("Bad character", TEL_BAD_CHAR)
        )

        private val demoText: String by lazy {
            val logger = Logger.getInstance(TemplateColorSettingsPage::class.java)

            val template = try {
                FileUtil.loadTextAndClose(
                    TemplateColorSettingsPage::class.java.getResourceAsStream("/com/github/rar91279/plugin/tapestry/templateColorSettingsText.html")
                ).also { if (it.isEmpty()) logger.warn("Template color settings demo text is empty") }
            }
            catch (ex: IOException) {
                logger.error(ex)
                ""
            }

            template.replace('\r', ' ')
        }
    }
}
