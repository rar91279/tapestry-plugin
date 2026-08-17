package com.github.rar91279.plugin.tapestry.intellij.view.nodes

import com.github.rar91279.plugin.tapestry.core.model.ioc.Service
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.module.Module

/**
 * One IoC service, shown under its id with the module class that declares it.
 *
 * The node's value is the declaring `build*` method, so opening it lands on the declaration rather than on the
 * service interface — which is usually shared by several implementations, and often not the module's own code.
 */
class ServiceNode(service: Service, module: Module) : TapestryNode(module) {

    init {
        val declaringClass = service.declaration?.containingClass?.name
        val suffix = buildList {
            declaringClass?.let { add(it) }
            if (service.scope.isNotEmpty()) add(service.scope)
            if (service.isEagerLoad) add("eager")
        }.joinToString(", ")

        init(
            service.declaration ?: service.serviceClass ?: service.id,
            PresentationData(service.id, suffix.ifEmpty { service.id }, AllIcons.Nodes.Method, null)
        )
    }
}
