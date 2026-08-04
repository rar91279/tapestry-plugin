package com.github.rar91279.plugin.tapestry.intellij.util

import com.intellij.facet.FacetManager
import com.intellij.javaee.web.WebRoot
import com.intellij.javaee.web.facet.WebFacet
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaArrayType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaClassType
import com.github.rar91279.plugin.tapestry.intellij.core.java.IntellijJavaPrimitiveType
import com.intellij.util.IncorrectOperationException
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Utility methods for IDEA.
 */
object IdeaUtils {

    /**
     * @return `true` if the given event was created from a module node, `false` otherwise.
     */
    @JvmStatic
    fun isModuleNode(event: AnActionEvent): Boolean =
        event.getData(CommonDataKeys.PROJECT) != null && event.getData(LangDataKeys.MODULE_CONTEXT) != null

    /**
     * @return all the module web roots except the WEB-INF directory.
     */
    @JvmStatic
    fun findWebRoots(module: Module): List<WebRoot> =
        getWebFacet(module)?.webRoots?.filter { it.relativePath != "/WEB-INF" } ?: emptyList()

    /**
     * Ensures that the given package exists in the given source directory.
     *
     * @return the new/existing directory.
     * @throws IncorrectOperationException if an error occurs executing.
     */
    @JvmStatic
    @Throws(IncorrectOperationException::class)
    fun findOrCreateDirectoryForPackage(sourceDirectory: PsiDirectory, packageName: String): PsiDirectory =
        packageName.split('.')
            .filter { it.isNotEmpty() }
            .fold(sourceDirectory) { directory, name ->
                directory.findSubdirectory(name) ?: directory.createSubdirectory(name)
            }

    /**
     * @return `true` if the given directory is a web root in the given module, `false` otherwise.
     */
    @JvmStatic
    fun isWebRoot(module: Module, directory: VirtualFile): Boolean =
        getWebFacet(module)?.webRoots?.any { directory == it.file } ?: false

    @JvmStatic
    fun findPublicClass(psiFile: PsiFile?): PsiClass? =
        (psiFile as? PsiClassOwner)?.let { findPublicClass(it.classes) }

    /**
     * @return the first public class in the given array of classes, `null` if none is found.
     */
    @JvmStatic
    fun findPublicClass(classes: Array<PsiClass>): PsiClass? = classes.firstOrNull { clazz ->
        clazz.isValid &&
        clazz.modifierList?.hasModifierProperty(PsiModifier.PUBLIC) == true &&
        !clazz.isEnum &&
        !clazz.isInterface &&
        PsiUtil.hasDefaultConstructor(clazz)
    }

    /**
     * Executes some code inside a write action command block.
     */
    @JvmStatic
    fun runWriteCommand(project: Project?, runnable: Runnable) {
        CommandProcessor.getInstance().executeCommand(
            project, { ApplicationManager.getApplication().runWriteAction(runnable) }, "", null
        )
    }

    /**
     * Finds the first parent node whose user object is of the given type.
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> findFirstParent(node: DefaultMutableTreeNode?, clazz: Class<T>): T? {
        var parent = node?.parent as DefaultMutableTreeNode?

        while (parent != null) {
            if (clazz.isInstance(parent.userObject)) return parent as T
            parent = parent.parent as DefaultMutableTreeNode?
        }

        return null
    }

    /**
     * Creates a JavaType instance from a PsiType.
     *
     * @return the corresponding JavaType instance, or `null` if the type can't be converted into a JavaType.
     */
    @JvmStatic
    fun createJavaTypeFromPsiType(module: Module, type: PsiType?): IJavaType? {
        when (type) {
            is PsiClassType -> {
                val psiClass = try {
                    val resolved = type.resolve()
                    if (resolved is PsiTypeParameter) { // let's consider generic type T as Object
                        JavaPsiFacade.getInstance(module.project)
                            .findClass("java.lang.Object", GlobalSearchScope.moduleWithLibrariesScope(module))
                    } else resolved
                } catch (ex: ProcessCanceledException) {
                    throw ex
                }

                return psiClass?.let { IntellijJavaClassType(module, it.containingFile) }
            }

            is PsiPrimitiveType -> return IntellijJavaPrimitiveType(type)
            is PsiArrayType -> return IntellijJavaArrayType(module, type)
            else -> return null
        }
    }

    /**
     * @return the web facet of the given module, or `null` if the module doesn't have one.
     */
    @JvmStatic
    fun getWebFacet(module: Module): WebFacet? = FacetManager.getInstance(module).getFacetByType(WebFacet.ID)

    @JvmStatic
    fun getPackage(psiElement: PsiElement?): PsiPackage? {
        if (psiElement is PsiDirectory) {
            val project = psiElement.project
            val packageName = ProjectRootManager.getInstance(project).fileIndex
                .getPackageNameByDirectory(psiElement.virtualFile) ?: return null

            return JavaPsiFacade.getInstance(project).findPackage(packageName)
        }

        return psiElement as? PsiPackage
    }

    @JvmStatic
    fun getNameElement(tag: XmlTag): XmlElement? = tag.firstChild.nextSiblings().filterIsInstance<XmlElement>().firstOrNull()

    @JvmStatic
    fun getNameElementClosing(tag: XmlTag): XmlElement? = tag.lastChild.prevSiblings().filterIsInstance<XmlElement>().firstOrNull()

    private fun PsiElement?.nextSiblings(): Sequence<PsiElement> = generateSequence(this) { it.nextSibling }.drop(1)

    private fun PsiElement?.prevSiblings(): Sequence<PsiElement> = generateSequence(this) { it.prevSibling }.drop(1)
}
