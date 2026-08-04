package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation

import com.github.rar91279.plugin.tapestry.core.model.externalizable.ExternalizableToDocumentation
import com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain.DocumentationGenerator

/**
 * The documentation home: the project modules and the Tapestry IoC services they contribute.
 */
class Home(val modules: List<ModuleDoc>) : ExternalizableToDocumentation {

    override val documentation: String?
        get() = DocumentationGenerator.generate(this)

    /** A project module and the Tapestry IoC services it contributes (empty for non-Tapestry modules). */
    data class ModuleDoc(val name: String, val isTapestry: Boolean, val services: List<ServiceDoc>)

    /** A Tapestry IoC service and its documentation. */
    data class ServiceDoc(
        val id: String,
        val className: String,
        val scope: String,
        val isEagerLoad: Boolean,
        val description: String
    )
}
