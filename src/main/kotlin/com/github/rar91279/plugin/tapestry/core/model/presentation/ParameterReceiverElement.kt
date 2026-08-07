package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.model.presentation.components.DummyTapestryParameter

/**
 * A presentation element that declares Tapestry parameters.
 */
abstract class ParameterReceiverElement internal constructor(
    library: TapestryLibrary?,
    elementClass: IJavaClassType,
    project: TapestryProject
) : PresentationLibraryElement(library, elementClass, project) {

    private var parametersCache: Map<String, TapestryParameter>? = null
    private var parametersCacheTimestamp: Long = 0

    /**
     * The declared Tapestry parameters, by name.
     */
    open val parameters: Map<String, TapestryParameter>
        get() {
            val lastModified = elementClass.file?.file?.lastModified() ?: 0
            parametersCache?.let { if (lastModified <= parametersCacheTimestamp) return it }

            val parameters = HashMap<String, TapestryParameter>()
            parameters["mixins"] = DummyTapestryParameter(project, "mixins", false)
            parametersCacheTimestamp = lastModified

            for (field in elementClass.getFields(true).values) {
                if (field.isPrivate && field.isValid && field.annotations.containsKey(PARAMETER_ANNOTATION)) {
                    val parameter = TapestryParameter(elementClass, field)
                    parameters[parameter.name] = parameter
                }
            }

            return parameters.toMap().also { parametersCache = it }
        }

    /** The declared Tapestry required parameters. */
    val requiredParameters: Map<String, TapestryParameter>
        get() = parameters.filterValues { it.isRequired }

    /** The declared Tapestry not required parameters. */
    val optionalParameters: Map<String, TapestryParameter>
        get() = parameters.filterValues { !it.isRequired }
}
