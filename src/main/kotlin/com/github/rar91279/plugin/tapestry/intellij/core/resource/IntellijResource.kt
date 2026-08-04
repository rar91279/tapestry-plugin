package com.github.rar91279.plugin.tapestry.intellij.core.resource

import com.intellij.psi.PsiFile
import com.intellij.psi.XmlRecursiveElementVisitor
import com.github.rar91279.plugin.tapestry.core.resource.CoreXmlRecursiveElementVisitor
import com.github.rar91279.plugin.tapestry.core.resource.IResource
import com.github.rar91279.plugin.tapestry.intellij.core.resource.xml.IntellijXmlTag
import java.io.File

/** [IResource] backed by an IntelliJ PSI file. */
class IntellijResource(val psiFile: PsiFile) : IResource {

    override val name: String
        get() = psiFile.name

    override val file: File
        get() = File(psiFile.viewProvider.virtualFile.path)

    override val extension: String?
        get() = psiFile.virtualFile.extension

    override fun accept(visitor: CoreXmlRecursiveElementVisitor) {
        psiFile.accept(object : XmlRecursiveElementVisitor() {
            override fun visitXmlTag(tag: com.intellij.psi.xml.XmlTag) {
                super.visitXmlTag(tag)

                visitor.visitTag(IntellijXmlTag(tag))
            }
        })
    }
}
