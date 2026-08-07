package com.github.rar91279.plugin.tapestry.core.model.ioc

import com.github.rar91279.plugin.tapestry.core.ioc.ServiceBinding
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType

/**
 * A Tapestry IoC service.
 */
class Service(serviceBinding: ServiceBinding, val serviceClass: IJavaClassType?) {

    val id: String = serviceBinding.id.orEmpty()
    val scope: String = serviceBinding.scope.orEmpty()
    val isEagerLoad: Boolean = serviceBinding.isEagerLoad

    override fun toString(): String = id
}
