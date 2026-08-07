package com.github.rar91279.plugin.tapestry.core.model.externalizable

import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.model.presentation.Page
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.util.PathUtils
import java.util.Locale

/**
 * Generates the representation of a presentation element to be included in a template.
 */
object TemplateExternalizer {

    /**
     * @param element         the element to generate the representation for.
     * @param namespacePrefix the Tapestry namespace prefix in the template where the representation is going to be included.
     * @return the element representation, or `null` if the element can't be externalized to a template.
     */
    @Throws(Exception::class)
    fun externalize(element: Any, namespacePrefix: String?): String? = when (element) {
        is TapestryComponent -> {
            val name = element.qualifiedElementName(separator = ".") { PathUtils.pathIntoPackage(it.lowercase(Locale.getDefault()), false) }
            val requiredParameters = element.parameters.values
                .filter { it.isRequired }
                .joinToString("") { " ${it.name}=\"\"" }

            "<$namespacePrefix:$name$requiredParameters></$namespacePrefix:$name>"
        }

        is Page -> {
            if (element.elementClass.file == null) throw RuntimeException("The page is invalid!!")

            val name = element.qualifiedElementName(separator = "/") { it }
            "<$namespacePrefix:pagelink page=\"$name\">Link to $name</$namespacePrefix:pagelink>"
        }

        else -> null
    }

    /** The element name, prefixed with its library id unless it comes from the application or core library. */
    private fun PresentationLibraryElement.qualifiedElementName(separator: String, transform: (String) -> String): String {
        val name = transform(name.orEmpty())
        val libraryId = library?.id
        return if (libraryId == TapestryProject.APPLICATION_LIBRARY_ID || libraryId == TapestryProject.CORE_LIBRARY_ID) {
            name
        }
        else {
            "$libraryId$separator$name"
        }
    }
}
