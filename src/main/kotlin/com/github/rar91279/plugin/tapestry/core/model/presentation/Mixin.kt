package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.resource.IResource

/**
 * A Tapestry mixin.
 */
class Mixin internal constructor(
    library: TapestryLibrary?,
    componentClass: IJavaClassType,
    project: TapestryProject
) : ParameterReceiverElement(library, componentClass, project) {

    override fun allowsTemplate(): Boolean = false

    override val template: Array<IResource> get() = emptyArray()

    override val messageCatalog: Array<IResource> get() = emptyArray()
}
