package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType

/** The built-in Parameter element. */
class ParameterComponent private constructor(componentClass: IJavaClassType, project: TapestryProject) :
    BuiltinComponent(
        componentClass, project, "parameter",
        mapOf("id" to DummyTapestryParameter(project, "name", true))
    ) {

    companion object {
        @JvmStatic
        fun getInstance(tapestryProject: TapestryProject): ParameterComponent? =
            tapestryProject.findBuiltinClass("ParameterToken")?.let { ParameterComponent(it, tapestryProject) }
    }
}
