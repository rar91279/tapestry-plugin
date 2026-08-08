package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.intellij.psi.PsiClass
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryParameter

/**
 * A built-in component: one that has no class of its own in the application, so its name and
 * parameters are hard-coded rather than derived from a class.
 */
class BuiltinComponent private constructor(
    componentClass: PsiClass,
    project: TapestryProject,
    override val name: String,
    override val parameters: Map<String, TapestryParameter>
) : TapestryComponent(componentClass, project) {

    override fun getElementNameFromClass(libraryRootPackage: String?): String = name

    companion object {

        /** Every built-in component available in [project], skipping any whose token class is absent. */
        fun all(project: TapestryProject): List<BuiltinComponent> = listOfNotNull(
            create(project, "body", "BodyToken"),
            create(project, "block", "BlockToken", mapOf("id" to DummyTapestryParameter(project, "id", false))),
            create(project, "parameter", "ParameterToken", mapOf("id" to DummyTapestryParameter(project, "name", true))),
            create(project, "container", "TemplateToken")
        )

        private fun create(
            project: TapestryProject,
            name: String,
            tokenClassName: String,
            parameters: Map<String, TapestryParameter> = emptyMap()
        ): BuiltinComponent? =
            project.findType("org.apache.tapestry5.internal.parser.$tokenClassName", true)
                ?.let { BuiltinComponent(it, project, name, parameters) }
    }
}
