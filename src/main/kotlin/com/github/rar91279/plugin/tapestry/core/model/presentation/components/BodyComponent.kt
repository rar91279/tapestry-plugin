package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType

/** The built-in Body element. */
class BodyComponent private constructor(componentClass: IJavaClassType, project: TapestryProject) :
    BuiltinComponent(componentClass, project, "body") {

    companion object {
        @JvmStatic
        fun getInstance(tapestryProject: TapestryProject): BodyComponent? =
            tapestryProject.findBuiltinClass("BodyToken")?.let { BodyComponent(it, tapestryProject) }
    }
}
