package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter
import com.intellij.psi.PsiType

/**
 * A dummy parameter, used to declare the parameters of the built-in components. It has no declaring
 * field, so every member that would read one is overridden here.
 */
class DummyTapestryParameter(tapestryProject: TapestryProject, name: String, required: Boolean) :
    TapestryParameter(null, null) {

    override val type: PsiType? = tapestryProject.findClassType("java.lang.String")

    override val name: String = name

    override val isRequired: Boolean = required

    override val defaultPrefix: String get() = "literal"
}
