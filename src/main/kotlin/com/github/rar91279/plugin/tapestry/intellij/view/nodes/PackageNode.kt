package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.github.rar91279.plugin.tapestry.core.exceptions.NotTapestryElementException
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.model.presentation.Page
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.intellij.TapestryModuleSupportLoader
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils
import com.github.rar91279.plugin.tapestry.intellij.view.TapestryProjectViewPane
import com.github.rar91279.plugin.tapestry.lang.TmlFileType
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.util.PlatformIcons
import java.util.TreeSet

open class PackageNode(private val library: TapestryLibrary?, psiDirectory: PsiDirectory, module: Module) : TapestryNode(module) {

    constructor(psiDirectory: PsiDirectory, module: Module) : this(null, psiDirectory, module)

    init {
        init(psiDirectory, PresentationData(psiDirectory.name, psiDirectory.name, PlatformIcons.PACKAGE_ICON, null))
    }

    override fun getChildren(): Array<SimpleNode> {
        val children = TreeSet<TapestryNode>(PackageNodesComparator)
        val directory = getValue() as PsiDirectory

        for (subdirectory in directory.subdirectories) {
            createNewNode(subdirectory)?.let { children.add(it) }
        }

        for (psiFile in directory.files) {
            if (psiFile is PsiClassOwner) {
                // A class that is not a Tapestry element — or element grouping being switched off — shows as
                // a plain class node. That used to be signalled by throwing NotTapestryElementException with
                // an empty message purely to reach the catch below; the real exception, thrown out of
                // createElementInstance, still has to be caught.
                val psiClass =
                    if (TapestryProjectViewPane.getInstance(myProject).isGroupElementFiles) IdeaUtils.findPublicClass(psiFile)
                    else null

                val element = psiClass?.let {
                    try {
                        val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module)!!
                        if (library == null) {
                            PresentationLibraryElement.createProjectElementInstance(it, tapestryProject)
                        } else {
                            PresentationLibraryElement.createElementInstance(library, it, tapestryProject)
                        }
                    } catch (e: NotTapestryElementException) {
                        null
                    }
                }

                when (element?.elementType) {
                    PresentationLibraryElement.ElementType.PAGE -> children.add(PageNode(element, module))
                    PresentationLibraryElement.ElementType.COMPONENT -> children.add(ComponentNode(element, module))
                    PresentationLibraryElement.ElementType.MIXIN -> children.add(MixinNode(element, module))
                    null -> children.add(ClassNode(psiFile, module))
                }
            }

            if (psiFile.fileType == TmlFileType && !TapestryProjectViewPane.getInstance(myProject).isGroupElementFiles) {
                children.add(FileNode(psiFile, module))
            }

            if (psiFile.fileType == PropertiesFileType.INSTANCE && !TapestryProjectViewPane.getInstance(myProject).isGroupElementFiles) {
                children.add(FileNode(psiFile, module))
            }
        }

        return children.map { it as SimpleNode }.toTypedArray()
    }

    private fun createNewNode(psiDirectory: PsiDirectory): PackageNode? {
        val aPackage = IdeaUtils.getPackage(psiDirectory) ?: return null
        val tapestryProject = TapestryModuleSupportLoader.getTapestryProject(module) ?: return null
        val applicationRootPackage = tapestryProject.applicationRootPackage
        val packageName = aPackage.qualifiedName

        return when {
            packageName == applicationRootPackage -> LibraryNode(tapestryProject.applicationLibrary!!, psiDirectory, module)
            packageName == tapestryProject.pagesRootPackage -> PagesNode(psiDirectory, module)
            packageName == tapestryProject.componentsRootPackage -> ComponentsNode(psiDirectory, module)
            packageName == tapestryProject.mixinsRootPackage -> MixinsNode(psiDirectory, module)
            else -> PackageNode(psiDirectory, module)
        }
    }
}
