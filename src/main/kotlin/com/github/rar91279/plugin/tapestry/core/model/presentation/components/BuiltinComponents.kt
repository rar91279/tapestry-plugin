package com.github.rar91279.plugin.tapestry.core.model.presentation.components

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaAnnotation
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.java.IJavaType
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

/** The built-in Block element. */
class BlockComponent private constructor(componentClass: IJavaClassType, project: TapestryProject) :
    BuiltinComponent(componentClass, project, "block", mapOf("id" to DummyTapestryParameter(project, "id", false))) {

    companion object {
        @JvmStatic
        fun getInstance(tapestryProject: TapestryProject): BlockComponent? =
            tapestryProject.findBuiltinClass("BlockToken")?.let { BlockComponent(it, tapestryProject) }
    }
}

/** The built-in Body element. */
class BodyComponent private constructor(componentClass: IJavaClassType, project: TapestryProject) :
    BuiltinComponent(componentClass, project, "body") {

    companion object {
        @JvmStatic
        fun getInstance(tapestryProject: TapestryProject): BodyComponent? =
            tapestryProject.findBuiltinClass("BodyToken")?.let { BodyComponent(it, tapestryProject) }
    }
}

/** The built-in Container element. */
class ContainerComponent private constructor(componentClass: IJavaClassType, project: TapestryProject) :
    BuiltinComponent(componentClass, project, "container") {

    companion object {
        @JvmStatic
        fun getInstance(tapestryProject: TapestryProject): ContainerComponent? =
            tapestryProject.findBuiltinClass("TemplateToken")?.let { ContainerComponent(it, tapestryProject) }
    }
}

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

private fun TapestryProject.findBuiltinClass(tokenClassName: String): IJavaClassType? =
    javaTypeFinder.findType("org.apache.tapestry5.internal.parser.$tokenClassName", true)

/**
 * A dummy parameter, used to declare the parameters of the built-in components.
 */
class DummyTapestryParameter(tapestryProject: TapestryProject, name: String, required: Boolean) :
    TapestryParameter(null, DummyJavaField(name, tapestryProject.javaTypeFinder.findType("java.lang.String", true))) {

    override val name: String = name

    override val isRequired: Boolean = required

    override val defaultPrefix: String get() = "literal"
}

/**
 * A dummy java field, backing a [DummyTapestryParameter].
 */
private class DummyJavaField(override val name: String, override val type: IJavaType?) : IJavaField {

    override val isPrivate: Boolean get() = true

    override val annotations: Map<String, IJavaAnnotation> get() = emptyMap()

    override val documentation: String get() = ""

    override val stringRepresentation: String get() = ""

    override val isValid: Boolean get() = true
}
