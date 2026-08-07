package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter

/**
 * A built-in component: one that has no class of its own in the application, so its name and
 * parameters are hard-coded rather than derived from a class.
 */
sealed class BuiltinComponent(
    componentClass: IJavaClassType,
    project: TapestryProject,
    override val name: String,
    override val parameters: Map<String, TapestryParameter> = emptyMap()
) : TapestryComponent(componentClass, project) {

    override fun getElementNameFromClass(libraryRootPackage: String?): String = name
}

internal fun TapestryProject.findBuiltinClass(tokenClassName: String): IJavaClassType? =
    javaTypeFinder.findType("org.apache.tapestry5.internal.parser.$tokenClassName", true)
