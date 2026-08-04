package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain

import com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.Home
import com.github.rar91279.plugin.tapestry.core.util.VelocityProcessor

/**
 * Renders the documentation page for a single Tapestry IoC service.
 */
object ServiceDocumentation {

    private const val TEMPLATE = "/documentation/service.vm"

    fun render(service: Home.ServiceDoc): String {
        val context = DocAssets.baseContext()
        context["service"] = service
        return VelocityProcessor.processClasspathTemplate(TEMPLATE, context)
    }
}
