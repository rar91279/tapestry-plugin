package com.github.rar91279.plugin.tapestry.core.model

import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.TapestryProject
import com.github.rar91279.plugin.tapestry.core.exceptions.NotTapestryElementException
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.intellij.util.containers.CollectionFactory

/**
 * Represents a Tapestry library.
 */
class TapestryLibrary @JvmOverloads constructor(
    val id: String?,
    val basePackage: String?,
    private val project: TapestryProject?,
    val shortName: String? = null
) : Comparable<TapestryLibrary> {

    /** All components of this library. */
    val components: Map<String, PresentationLibraryElement>
        get() = findElements(TapestryConstants.COMPONENTS_PACKAGE)

    /** All abstract components of this library. */
    val abstractComponents: Map<String, PresentationLibraryElement>
        get() = findElements(TapestryConstants.BASE_PACKAGE)

    /** All pages of this library. */
    val pages: Map<String, PresentationLibraryElement>
        get() = findElements(TapestryConstants.PAGES_PACKAGE)

    /** All mixins of this library. */
    val mixins: Map<String, PresentationLibraryElement>
        get() = findElements(TapestryConstants.MIXINS_PACKAGE)

    override fun compareTo(other: TapestryLibrary): Int = basePackage.orEmpty().compareTo(other.basePackage.orEmpty())

    override fun equals(other: Any?): Boolean = other is TapestryLibrary && basePackage == other.basePackage

    override fun hashCode(): Int = basePackage.hashCode()

    /**
     * Finds all Tapestry elements implemented under the given sub package of this library's base package.
     */
    private fun findElements(componentsOrPages: String): Map<String, PresentationLibraryElement> {
        val elements = CollectionFactory.createCaseInsensitiveStringMap<PresentationLibraryElement>()
        val typeFinder = project ?: return elements

        for (type in typeFinder.findTypesInPackageRecursively("$basePackage.$componentsOrPages", true)) {
            try {
                val element = PresentationLibraryElement.createElementInstance(this, type, project)
                elements[element.name ?: continue] = element
            }
            catch (e: NotTapestryElementException) {
                //ignore
            }
        }

        return elements
    }
}
