package com.github.rar91279.plugin.tapestry.core.model.externalizable

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.java.IJavaClassType
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.Page
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.util.PathUtils

/**
 * Generates the representation of a presentation element to be included in a class, as an annotated field.
 */
object ClassExternalizer {

    private val logger = Logger.getInstance(ClassExternalizer::class.java)

    /**
     * @param element     the element to generate the representation for.
     * @param targetClass the class where the representation is going to be included.
     * @return the element representation, or `null` if the element can't be externalized to a class.
     */
    @Throws(Exception::class)
    fun externalize(element: Any, targetClass: IJavaClassType): String? = try {
        when (element) {
            // a component field carries the required parameters of the component
            is TapestryComponent -> {
                val parameters = element.parameters.values
                    .filter { it.isRequired }
                    .joinToString(",") { "\"${it.name}=\"" }

                element.externalizeAsField(
                    targetClass,
                    fieldName = StringUtil.notNullize(element.elementClass.name),
                    annotation = TapestryConstants.COMPONENT_ANNOTATION,
                    annotationParameters = if (parameters.isEmpty()) emptyMap() else mapOf("parameters" to "{$parameters}")
                )
            }

            is Page -> element.externalizeAsField(
                targetClass,
                fieldName = PathUtils.getLastPathElement(element.name),
                annotation = TapestryConstants.INJECT_PAGE_ANNOTATION
            )

            is Mixin -> element.externalizeAsField(
                targetClass,
                fieldName = PathUtils.getLastPathElement(element.name),
                annotation = TapestryConstants.MIXIN_ANNOTATION
            )

            else -> null
        }
    }
    catch (ex: Exception) {
        logger.error(ex)
        throw ex
    }

    private fun PresentationLibraryElement.externalizeAsField(
        targetClass: IJavaClassType,
        fieldName: String?,
        annotation: String,
        annotationParameters: Map<String, String> = emptyMap()
    ): String? {
        val typeCreator = project.javaTypeCreator
        val takenNames = targetClass.getFields(false).keys

        var field = typeCreator.createField(fieldName.orEmpty(), elementClass, true, true) ?: return null
        val suggestedName = suggestName(field.name.orEmpty(), takenNames)
        if (suggestedName != field.name) {
            field = typeCreator.createField(suggestedName, elementClass, true, true) ?: return null
        }

        typeCreator.createFieldAnnotation(field, annotation, annotationParameters)

        var serialized = field.stringRepresentation ?: return null

        // use the short names of the classes that could be imported into the target class
        if (typeCreator.ensureClassImport(targetClass, elementClass)) {
            elementClass.fullyQualifiedName?.let { fqn -> serialized = serialized.replace(fqn, elementClass.name.orEmpty()) }
        }

        val annotationType = project.javaTypeFinder.findType(annotation, true)
        if (annotationType != null && typeCreator.ensureClassImport(targetClass, annotationType)) {
            serialized = serialized.replace(annotation, annotation.substringAfterLast('.'))
        }

        return "\n$serialized\n"
    }

    /**
     * Suggests a name for a field: the desired name, or, if that one is taken, the desired name
     * with the lowest free number suffix.
     */
    private fun suggestName(desiredName: String, takenNames: Set<String>): String {
        if (desiredName !in takenNames) return desiredName

        return generateSequence(1) { it + 1 }.map { "$desiredName$it" }.first { it !in takenNames }
    }
}
