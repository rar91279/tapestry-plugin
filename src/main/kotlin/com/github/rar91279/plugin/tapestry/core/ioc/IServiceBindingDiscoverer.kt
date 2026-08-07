package com.github.rar91279.plugin.tapestry.core.ioc

import com.github.rar91279.plugin.tapestry.core.java.IJavaMethod

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
