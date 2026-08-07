package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType

/** The built-in Container element. */
class ContainerComponent private constructor(componentClass: IJavaClassType, project: TapestryProject) :
    BuiltinComponent(componentClass, project, "container") {

    companion object {
        @JvmStatic
        fun getInstance(tapestryProject: TapestryProject): ContainerComponent? =
            tapestryProject.findBuiltinClass("TemplateToken")?.let { ContainerComponent(it, tapestryProject) }
    }
}
