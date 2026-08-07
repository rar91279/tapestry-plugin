package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter

/**
 * A dummy parameter, used to declare the parameters of the built-in components.
 */
class DummyTapestryParameter(tapestryProject: TapestryProject, name: String, required: Boolean) :
    TapestryParameter(null, DummyJavaField(name, tapestryProject.javaTypeFinder.findType("java.lang.String", true))) {

    override val name: String = name

    override val isRequired: Boolean = required

    override val defaultPrefix: String get() = "literal"
}
