package com.github.rar91279.plugin.tapestry.intellij.lang.descriptor

import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.intellij.xml.XmlElementDescriptor
import com.intellij.xml.impl.schema.SchemaNSDescriptor

/** Namespace descriptor of the Tapestry template namespace. */
class TapestryNamespaceDescriptor : SchemaNSDescriptor() {

    private var file: XmlFile? = null
    private var element: XmlElement? = null

    override fun getElementDescriptor(tag: XmlTag): XmlElementDescriptor? = DescriptorUtil.getTmlOrHtmlTagDescriptor(tag)

    override fun getRootElementsDescriptors(doc: XmlDocument?): Array<XmlElementDescriptor> {
        val rootTag = doc?.rootTag ?: return XmlElementDescriptor.EMPTY_ARRAY

        return DescriptorUtil.getTmlSubelementDescriptors(rootTag, this)
    }

    override fun getDescriptorFile(): XmlFile? = file

    override fun getDeclaration(): PsiElement? = element

    override fun getName(context: PsiElement?): String = name

    override fun getName(): String = file?.name.orEmpty()

    override fun init(element: PsiElement) {
        super.init(element)

        file = element.containingFile as? XmlFile
        this.element = if (element is XmlDocument) element.rootTag else element as? XmlElement
    }

    override fun getDependencies(): Array<Any> = TapestryProject.JAVA_STRUCTURE_DEPENDENCY

    fun getSuperRootElementsDescriptors(document: XmlDocument?): Array<XmlElementDescriptor> =
        super.getRootElementsDescriptors(document)
}
