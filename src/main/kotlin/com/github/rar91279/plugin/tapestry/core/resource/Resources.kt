package com.github.rar91279.plugin.tapestry.core.resource

import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlTag
import java.io.File

/**
 * A visitor for XML files.
 */
interface CoreXmlRecursiveElementVisitor {

    fun visitTag(tag: XmlTag)
}

/**
 * Represents a resource in the application web context.
 */
interface IResource {

    /** The resource file name. */
    val name: String

    /** The file behind this resource. */
    val file: File?

    /** The file extension without the '.'. */
    val extension: String?

    /**
     * Starts the visitor pattern execution.
     * If this resource is not a XML file then this should do nothing.
     */
    fun accept(visitor: CoreXmlRecursiveElementVisitor)
}

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
