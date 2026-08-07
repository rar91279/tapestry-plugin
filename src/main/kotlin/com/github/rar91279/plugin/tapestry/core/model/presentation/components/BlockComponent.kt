package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType

/** The built-in Block element. */
class BlockComponent private constructor(componentClass: IJavaClassType, project: TapestryProject) :
    BuiltinComponent(componentClass, project, "block", mapOf("id" to DummyTapestryParameter(project, "id", false))) {

    companion object {
        @JvmStatic
        fun getInstance(tapestryProject: TapestryProject): BlockComponent? =
            tapestryProject.findBuiltinClass("BlockToken")?.let { BlockComponent(it, tapestryProject) }
    }
}
