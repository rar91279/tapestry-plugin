package com.github.rar91279.plugin.tapestry.core.resource

/**
 * Finds resources.
 */
interface IResourceFinder {

    /**
     * Looks up resources in the classpath.
     */
    fun findClasspathResource(path: String, includeDependencies: Boolean): Collection<IResource>

    /**
     * Looks up localized resources in the classpath.
     */
    fun findLocalizedClasspathResource(path: String, includeDependencies: Boolean): Collection<IResource>

    /**
     * Looks up a resource in the web context.
     *
     * @return the resource in the given path, `null` if none is found.
     */
    fun findContextResource(path: String): IResource?

    /**
     * Looks up a localized resource in the web context.
     */
    fun findLocalizedContextResource(path: String): Collection<IResource>
}
