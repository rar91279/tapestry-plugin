package com.github.rar91279.plugin.tapestry.core.resource

import java.io.File

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
