package com.github.rar91279.plugin.tapestry.core.resource

import com.intellij.psi.PsiFile

/**
 * Finds resources (templates, message catalogs, ...) on a module's classpath and in its web roots.
 */
interface IResourceFinder {

    /**
     * Looks up resources in the classpath.
     */
    fun findClasspathResource(path: String, includeDependencies: Boolean): Collection<PsiFile>

    /**
     * Looks up localized resources in the classpath.
     */
    fun findLocalizedClasspathResource(path: String, includeDependencies: Boolean): Collection<PsiFile>

    /**
     * Looks up a resource by its path relative to a source or resource root.
     *
     * Unlike [findClasspathResource] this doesn't go through the package index, so it also reaches paths no
     * package can name — `META-INF/assets/…`, where Tapestry 5.4 and later keep classpath assets.
     */
    fun findRootRelativeResource(path: String): Collection<PsiFile>

    /**
     * Looks up a resource in the web context.
     *
     * @return the resource in the given path, `null` if none is found.
     */
    fun findContextResource(path: String): PsiFile?

    /**
     * Looks up a localized resource in the web context.
     */
    fun findLocalizedContextResource(path: String): Collection<PsiFile>
}
