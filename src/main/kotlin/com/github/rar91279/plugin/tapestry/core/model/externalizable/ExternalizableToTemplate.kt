package com.github.rar91279.plugin.tapestry.core.model.externalizable

/**
 * Every class that implements this has a representation that can be included in a template.
 */
interface ExternalizableToTemplate {

    @Throws(Exception::class)
    fun getTemplateRepresentation(namespacePrefix: String?): String?
}
