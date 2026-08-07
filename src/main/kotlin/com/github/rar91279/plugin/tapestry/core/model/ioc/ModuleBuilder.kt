package com.github.rar91279.plugin.tapestry.core.model.ioc

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.ioc.ServiceBinding
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod

/**
 * A Tapestry IoC module builder.
 */
class ModuleBuilder(private val moduleBuilderClass: IJavaClassType, private val project: TapestryProject?) {

    private var servicesCache: Collection<Service>? = null
    private var servicesCacheTimestamp: Long = 0

    /**
     * All services the module declares.
     */
    val services: Collection<Service>
        get() {
            val lastModified = moduleBuilderClass.file?.file?.lastModified() ?: 0
            servicesCache?.let { if (lastModified <= servicesCacheTimestamp) return it }

            val services = ArrayList<Service>()
            for (method in moduleBuilderClass.getPublicMethods(true)) {
                if (method.returnType !is IJavaClassType) continue

                // Default service building (build methods)
                if (method.name?.matches(TapestryConstants.SERVICE_BUILDER_METHOD_REGEXP.toRegex()) == true) {
                    services.add(serviceFromBuildMethod(method))
                }

                // Autobuilding
                if (method.name == TapestryConstants.SERVICE_AUTOBUILDER_METHOD_NAME) {
                    services.addAll(servicesFromBindMethod(method))
                }
            }

            servicesCacheTimestamp = lastModified
            return services.also { servicesCache = it }
        }

    private fun serviceFromBuildMethod(method: IJavaMethod): Service {
        val binding = ServiceBinding()

        method.getAnnotation(SCOPE_ANNOTATION)?.parameters?.get("value")?.firstOrNull()?.let { binding.scope = it }
        if (method.getAnnotation(EAGERLOAD_ANNOTATION) != null) binding.isEagerLoad = true

        val methodName = method.name.orEmpty()
        binding.id = if (methodName == TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX) method.returnType?.name
        else methodName.substring(TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX.length)

        return Service(binding, method.returnType as IJavaClassType)
    }

    private fun servicesFromBindMethod(method: IJavaMethod): Collection<Service> {
        val discoverer = project?.javaTypeFinder?.serviceBindingDiscoverer ?: return emptyList()

        return discoverer.getServiceBindings(method).map { Service(it, method.returnType as IJavaClassType) }
    }

    companion object {
        private const val SCOPE_ANNOTATION = "org.apache.tapestry5.ioc.annotations.Scope"
        private const val EAGERLOAD_ANNOTATION = "org.apache.tapestry5.ioc.annotations.EagerLoad"
    }
}
