package com.github.rar91279.plugin.tapestry.intellij.core.resource

import com.intellij.javaee.web.WebRoot
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.PsiFile
import com.github.rar91279.plugin.tapestry.core.resource.IResourceFinder
import com.github.rar91279.plugin.tapestry.core.util.LocalizationUtils
import com.github.rar91279.plugin.tapestry.core.util.PathUtils
import com.github.rar91279.plugin.tapestry.intellij.util.IdeaUtils

/** [IResourceFinder] that searches a module's classpath and web roots. */
class IntellijResourceFinder(private val module: Module) : IResourceFinder {

    override fun findClasspathResource(path: String, includeDependencies: Boolean): Collection<PsiFile> {
        val filename = PathUtils.getLastPathElement(path)

        return findPackageDirectories(path, includeDependencies)
            .mapNotNull { it.findFile(filename) }
            .toList()
    }

    override fun findLocalizedClasspathResource(path: String, includeDependencies: Boolean): Collection<PsiFile> {
        val filename = PathUtils.getLastPathElement(path)

        return findPackageDirectories(path, includeDependencies)
            .flatMap { it.files.asSequence() }
            .filter { LocalizationUtils.unlocalizeFileName(it.name) == filename }
            .toList()
    }

    override fun findContextResource(path: String): PsiFile? =
        webRoots().firstNotNullOfOrNull { webRoot ->
            webRoot.file
                ?.findFileByRelativePath(relativeTo(webRoot, path))
                ?.let { PsiManager.getInstance(module.project).findFile(it) }
        }

    override fun findLocalizedContextResource(path: String): Collection<PsiFile> {
        val filename = PathUtils.getLastPathElement(path)
        val resources = ArrayList<PsiFile>()

        for (webRoot in webRoots()) {
            val parentPath = PathUtils.removeLastFilePathElement(relativeTo(webRoot, path), true)

            val virtualFile = webRoot.file
            val parentVirtualFile =
                if (parentPath.isNotEmpty()) virtualFile?.findFileByRelativePath(parentPath) else virtualFile

            parentVirtualFile?.children
                ?.filter { LocalizationUtils.unlocalizeFileName(it.name) == filename }
                ?.mapNotNull { PsiManager.getInstance(module.project).findFile(it) }
                ?.forEach { resources.add(it) }
        }

        return resources
    }

    private fun webRoots(): List<WebRoot> = IdeaUtils.getWebFacet(module)?.webRoots ?: emptyList()

    /** The given path relative to the web root it lives in. */
    private fun relativeTo(webRoot: WebRoot, path: String): String {
        val unixPath = PathUtils.toUnixPath(path) ?: return path
        val rootPath = webRoot.relativePath ?: return path
        if (!unixPath.startsWith(rootPath)) return path

        return unixPath.substring(rootPath.length + if (rootPath.endsWith("/")) 0 else 1)
    }

    private fun findPackageDirectories(path: String, includeDependencies: Boolean): Sequence<PsiDirectory> {
        val psiPackage = JavaPsiFacade.getInstance(module.project).findPackage(PathUtils.pathIntoPackage(path, true))
            ?: return emptySequence()

        return psiPackage.getDirectories(searchScope(includeDependencies)).asSequence()
    }

    private fun searchScope(includeDependencies: Boolean): GlobalSearchScope =
        if (includeDependencies) GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        else GlobalSearchScope.moduleScope(module)
}
