package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.github.rar91279.plugin.tapestry.core.util.tapestryFields
import com.github.rar91279.plugin.tapestry.core.model.TapestryLibrary
import com.github.rar91279.plugin.tapestry.core.model.presentation.components.DummyTapestryParameter

/**
 * A presentation element that declares Tapestry parameters.
 */
abstract class ParameterReceiverElement internal constructor(
    library: TapestryLibrary?,
    elementClass: PsiClass,
    project: TapestryProject
) : PresentationLibraryElement(library, elementClass, project) {

    private var parametersCache: Map<String, TapestryParameter>? = null
    private var parametersCacheTimestamp: Long = 0

    /**
     * The declared Tapestry parameters, by name.
     */
    open val parameters: Map<String, TapestryParameter>
        get() {
            val lastModified = elementClass?.containingFile?.virtualFile?.timeStamp ?: 0L
            parametersCache?.let { if (lastModified <= parametersCacheTimestamp) return it }

            val parameters = HashMap<String, TapestryParameter>()
            parameters["mixins"] = DummyTapestryParameter(project, "mixins", false)
            parametersCacheTimestamp = lastModified

            for (field in elementClass?.tapestryFields(true).orEmpty().values) {
                if (field.hasModifierProperty(PsiModifier.PRIVATE) && field.isValid && field.hasAnnotation(PARAMETER_ANNOTATION)) {
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
