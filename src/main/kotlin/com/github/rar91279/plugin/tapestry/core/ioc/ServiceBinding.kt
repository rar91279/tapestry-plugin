package com.github.rar91279.plugin.tapestry.core.ioc

import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod

/**
 * A service binding that is done with service autobuilding.
 */
class ServiceBinding {
    var serviceClass: IJavaClassType? = null
    var isEagerLoad: Boolean = false
    var scope: String? = null
    var id: String? = null
}

/**
 * Finds all service bindings done using autobuilding.
 */
interface IServiceBindingDiscoverer {

    /**
     * @param method the bind method.
     * @return all service bindings.
     */
    fun getServiceBindings(method: IJavaMethod): Collection<ServiceBinding>
}
