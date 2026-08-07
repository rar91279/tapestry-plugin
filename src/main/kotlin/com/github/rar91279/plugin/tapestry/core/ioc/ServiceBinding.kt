package com.github.rar91279.plugin.tapestry.core.ioc

import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType

/**
 * A service binding that is done with service autobuilding.
 */
class ServiceBinding {
    var serviceClass: IJavaClassType? = null
    var isEagerLoad: Boolean = false
    var scope: String? = null
    var id: String? = null
}
