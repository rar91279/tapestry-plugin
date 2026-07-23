package com.intellij.tapestry.tests.mocks

import com.intellij.lang.FileASTNode
import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.SearchScope
import javax.swing.Icon

class PsiFileMock : PsiFile {

    private var _virtualFile: VirtualFile? = null
    private var _valid = false

    override fun getVirtualFile(): VirtualFile? = _virtualFile

    fun setVirtualFile(virtualFile: VirtualFile?): PsiFileMock {
        _virtualFile = virtualFile
        return this
    }

    override fun getContainingDirectory(): PsiDirectory? = null

    override fun getModificationStamp(): Long = 0

    override fun getOriginalFile(): PsiFile = this

    override fun getFileType(): FileType = throw UnsupportedOperationException()

    override fun getPsiRoots(): Array<PsiFile> = PsiFile.EMPTY_ARRAY

    override fun getViewProvider(): FileViewProvider = throw UnsupportedOperationException()

    override fun getNode(): FileASTNode? = null

    override fun isEquivalentTo(another: PsiElement?): Boolean = this === another

    override fun subtreeChanged() {
    }

    override fun checkSetName(name: String?) {
    }

    override fun getName(): String = throw UnsupportedOperationException()

    override fun setName(name: String): PsiElement? = null

    override fun getProject(): Project = throw UnsupportedOperationException()

    override fun getLanguage(): Language = throw UnsupportedOperationException()

    override fun getManager(): PsiManager? = null

    override fun getChildren(): Array<PsiElement> = PsiElement.EMPTY_ARRAY

    override fun getParent(): PsiDirectory? = null

    override fun getFirstChild(): PsiElement? = null

    override fun getLastChild(): PsiElement? = null

    override fun getNextSibling(): PsiElement? = null

    override fun getPrevSibling(): PsiElement? = null

    override fun getContainingFile(): PsiFile? = null

    override fun getTextRange(): TextRange? = null

    override fun getStartOffsetInParent(): Int = 0

    override fun getTextLength(): Int = 0

    override fun findElementAt(offset: Int): PsiElement? = null

    override fun findReferenceAt(offset: Int): PsiReference? = null

    override fun getTextOffset(): Int = 0

    override fun getText(): String? = null

    override fun textToCharArray(): CharArray = CharArray(0)

    override fun getNavigationElement(): PsiElement? = null

    override fun getOriginalElement(): PsiElement? = null

    override fun textMatches(text: CharSequence): Boolean = false

    override fun textMatches(element: PsiElement): Boolean = false

    override fun textContains(c: Char): Boolean = false

    override fun accept(visitor: PsiElementVisitor) {
    }

    override fun acceptChildren(visitor: PsiElementVisitor) {
    }

    override fun copy(): PsiElement? = null

    override fun add(element: PsiElement): PsiElement? = null

    override fun addBefore(element: PsiElement, anchor: PsiElement?): PsiElement? = null

    override fun addAfter(element: PsiElement, anchor: PsiElement?): PsiElement? = null

    override fun checkAdd(element: PsiElement) {
    }

    override fun addRange(first: PsiElement?, last: PsiElement?): PsiElement? = null

    override fun addRangeBefore(first: PsiElement, last: PsiElement, anchor: PsiElement?): PsiElement? = null

    override fun addRangeAfter(first: PsiElement?, last: PsiElement?, anchor: PsiElement?): PsiElement? = null

    override fun delete() {
    }

    override fun checkDelete() {
    }

    override fun deleteChildRange(first: PsiElement?, last: PsiElement?) {
    }

    override fun replace(newElement: PsiElement): PsiElement? = null

    override fun isValid(): Boolean = _valid

    fun setValid(valid: Boolean): PsiFileMock {
        _valid = valid
        return this
    }

    override fun isWritable(): Boolean = false

    override fun getReference(): PsiReference? = null

    override fun getReferences(): Array<PsiReference> = PsiReference.EMPTY_ARRAY

    override fun <T> getCopyableUserData(key: Key<T>): T? = null

    override fun <T> putCopyableUserData(key: Key<T>, value: T?) {
    }

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean = false

    override fun getContext(): PsiElement? = null

    override fun isPhysical(): Boolean = false

    override fun getResolveScope(): GlobalSearchScope = throw UnsupportedOperationException()

    override fun getUseScope(): SearchScope = throw UnsupportedOperationException()

    override fun <T> getUserData(key: Key<T>): T? = null

    override fun <T> putUserData(key: Key<T>, value: T?) {
    }

    override fun getIcon(flags: Int): Icon? = null

    override fun getPresentation(): ItemPresentation? = null

    override fun navigate(requestFocus: Boolean) {
    }

    override fun canNavigate(): Boolean = false

    override fun canNavigateToSource(): Boolean = false

    override fun isDirectory(): Boolean = false

    override fun processChildren(processor: PsiElementProcessor<in PsiFileSystemItem>): Boolean = false
}
