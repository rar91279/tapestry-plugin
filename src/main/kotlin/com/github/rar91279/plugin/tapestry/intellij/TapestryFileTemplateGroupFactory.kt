package com.github.rar91279.plugin.tapestry.intellij

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.highlighter.XmlFileType
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import icons.TapestryIcons

class TapestryFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {

    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val javaIcon = JavaFileType.INSTANCE.icon
        val tapestryIcon = TapestryIcons.Tapestry_logo_small
        return FileTemplateGroupDescriptor("Tapestry", tapestryIcon).apply {
            addTemplate(FileTemplateDescriptor(TapestryConstants.MODULE_BUILDER_CLASS_TEMPLATE_NAME, javaIcon))
            addTemplate(FileTemplateDescriptor(TapestryConstants.COMPONENT_CLASS_TEMPLATE_NAME, javaIcon))
            addTemplate(FileTemplateDescriptor(TapestryConstants.COMPONENT_TEMPLATE_TEMPLATE_NAME, tapestryIcon))
            addTemplate(FileTemplateDescriptor(TapestryConstants.MIXIN_CLASS_TEMPLATE_NAME, javaIcon))
            addTemplate(FileTemplateDescriptor(TapestryConstants.PAGE_CLASS_TEMPLATE_NAME, javaIcon))
            addTemplate(FileTemplateDescriptor(TapestryConstants.PAGE_TEMPLATE_TEMPLATE_NAME, tapestryIcon))
            addTemplate(FileTemplateDescriptor(TapestryConstants.POM_TEMPLATE_NAME, XmlFileType.INSTANCE.icon))
        }
    }
}
