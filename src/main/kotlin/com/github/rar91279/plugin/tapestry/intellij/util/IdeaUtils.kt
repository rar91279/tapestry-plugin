package com.github.rar91279.plugin.tapestry.intellij.util

import com.intellij.psi.PsiManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileDocumentManager
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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaDirectoryService
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
import com.intellij.util.IncorrectOperationException
import com.intellij.ui.treeStructure.SimpleNode
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/**
 * Utility methods for IDEA.
 */
object IdeaUtils {

    /**
     * @return `true` if the given event was created from a module node, `false` otherwise.
     */
    fun isModuleNode(event: AnActionEvent): Boolean =
        event.getData(CommonDataKeys.PROJECT) != null && event.getData(LangDataKeys.MODULE_CONTEXT) != null

    /**
     * @return all the module web roots except the WEB-INF directory.
     */
    fun findWebRoots(module: Module): List<WebRoot> =
        getWebFacet(module)?.webRoots?.filter { it.relativePath != "/WEB-INF" } ?: emptyList()

    /**
     * Ensures that the given package exists in the given source directory.
     *
     * @return the new/existing directory.
     * @throws IncorrectOperationException if an error occurs executing.
     */
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
    fun isWebRoot(module: Module, directory: VirtualFile): Boolean =
        getWebFacet(module)?.webRoots?.any { directory == it.file } ?: false

    fun findPublicClass(psiFile: PsiFile?): PsiClass? =
        (psiFile as? PsiClassOwner)?.let { findPublicClass(it.classes) }

    /**
     * @return the first public class in the given array of classes, `null` if none is found.
     */
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
    fun runWriteCommand(project: Project?, runnable: Runnable) {
        CommandProcessor.getInstance().executeCommand(
            project, { ApplicationManager.getApplication().runWriteAction(runnable) }, "", null
        )
    }

    /**
     * Finds the first parent node whose user object is of the given type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> findFirstParent(node: DefaultMutableTreeNode?, clazz: Class<T>): T? {
        var parent = node?.parent as? DefaultMutableTreeNode

        while (parent != null) {
            if (clazz.isInstance(parent.userObject)) return parent as T
            parent = parent.parent as? DefaultMutableTreeNode
        }

        return null
    }

    /**
     * The node a tree path points at, or `null` when the path holds something else.
     *
     * Not every component of a platform tree path is a [DefaultMutableTreeNode]: a tree that restores itself
     * from a cached presentation hands out `CachedTreePresentationNode` placeholders until the real nodes are
     * built, and those carry no user object at all. Reading a selection therefore has to tolerate a miss —
     * a hard cast throws a `ClassCastException` on the first selection after startup.
     */
    fun nodeOf(path: TreePath?): DefaultMutableTreeNode? = path?.lastPathComponent as? DefaultMutableTreeNode

    /** The Tapestry node a tree path points at, or `null` if the path points at something else. */
    fun tapestryNodeOf(path: TreePath?): SimpleNode? = nodeOf(path)?.userObject as? SimpleNode

    /**
     * @return the web facet of the given module, or `null` if the module doesn't have one.
     */
    fun getWebFacet(module: Module): WebFacet? = FacetManager.getInstance(module).getFacetByType(WebFacet.ID)

    fun getPackage(psiElement: PsiElement?): PsiPackage? =
        if (psiElement is PsiDirectory) JavaDirectoryService.getInstance().getPackage(psiElement)
        else psiElement as? PsiPackage

    fun getNameElement(tag: XmlTag): XmlElement? = tag.firstChild.nextSiblings().filterIsInstance<XmlElement>().firstOrNull()

    fun getNameElementClosing(tag: XmlTag): XmlElement? = tag.lastChild.prevSiblings().filterIsInstance<XmlElement>().firstOrNull()

    private fun PsiElement?.nextSiblings(): Sequence<PsiElement> = generateSequence(this) { it.nextSibling }.drop(1)

    private fun PsiElement?.prevSiblings(): Sequence<PsiElement> = generateSequence(this) { it.prevSibling }.drop(1)
}

/**
 * The PSI file open in the project's selected editor, or `null` if there is no editor, no file behind its
 * document, or no PSI for that file.
 *
 * Replaces three copies of the same `selectedTextEditor!!.document` → `getFile(...)!!` → `findFile(...)!!`
 * chain, each of which threw a NullPointerException rather than degrading when any link was absent.
 */
fun currentPsiFileInEditor(project: Project): PsiFile? {
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
    val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    return PsiManager.getInstance(project).findFile(virtualFile)
}
