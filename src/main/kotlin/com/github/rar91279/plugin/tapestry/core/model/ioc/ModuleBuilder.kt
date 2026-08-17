package com.github.rar91279.plugin.tapestry.core.model.ioc

import com.github.rar91279.plugin.tapestry.core.util.attributeValues
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.github.rar91279.plugin.tapestry.core.util.publicMethods
import com.intellij.psi.PsiMethod

/**
 * A Tapestry IoC module builder.
 */
class ModuleBuilder(private val moduleBuilderClass: PsiClass) {

    private var servicesCache: Collection<Service>? = null
    private var servicesCacheTimestamp: Long = 0

    /**
     * All services the module declares.
     */
    val services: Collection<Service>
        get() {
            val lastModified = moduleBuilderClass.containingFile?.virtualFile?.timeStamp ?: 0L
            servicesCache?.let { if (lastModified <= servicesCacheTimestamp) return it }

            val services = ArrayList<Service>()
            for (method in moduleBuilderClass.publicMethods(true)) {
                val returnClass = (method.returnType as? PsiClassType)?.resolve() ?: continue

                // Default service building (build methods)
                if (method.name.matches(TapestryConstants.SERVICE_BUILDER_METHOD_REGEXP.toRegex())) {
                    services.add(serviceFromBuildMethod(method, returnClass))
                }
            }

            servicesCacheTimestamp = lastModified
            return services.also { servicesCache = it }
        }

    private fun serviceFromBuildMethod(method: PsiMethod, returnClass: PsiClass): Service {
        val methodName = method.name
        val id = if (methodName == TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX) returnClass.name
        else methodName.substring(TapestryConstants.SERVICE_BUILDER_METHOD_PREFIX.length)

        return Service(
            id = id.orEmpty(),
            scope = method.getAnnotation(SCOPE_ANNOTATION)?.attributeValues("value")?.firstOrNull().orEmpty(),
            isEagerLoad = method.getAnnotation(EAGERLOAD_ANNOTATION) != null,
            serviceClass = returnClass,
            declaration = method,
        )
    }

    companion object {
        private const val SCOPE_ANNOTATION = "org.apache.tapestry5.ioc.annotations.Scope"
        private const val EAGERLOAD_ANNOTATION = "org.apache.tapestry5.ioc.annotations.EagerLoad"
    }
}
