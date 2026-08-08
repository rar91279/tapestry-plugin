package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.github.rar91279.plugin.tapestry.core.util.attributeValues
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.intellij.psi.PsiField
import com.intellij.psi.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.util.ClassUtils

/**
 * Represents an injected element in a Tapestry application.
 *
 * An injected element can be one of two types:
 * - A Java field annotated with `@Component` or `@InjectPage`
 * - A component tag found in a Tapestry template XML file
 *
 * This class provides a unified way to work with both types of injected elements,
 * extracting information such as element IDs and parameters from either source.
 */
class InjectedElement : Comparable<InjectedElement> {

    /** The Java field representing this injected element, or null if this is a tag-based element. */
    val field: PsiField?

    /** The XML tag representing this injected element, or null if this is a field-based element. */
    val tag: XmlTag?

    /** The presentation library element associated with this injected element. */
    val element: PresentationLibraryElement?

    /**
     * Creates an injected element from a Java field.
     *
     * @param field the Java field annotated with `@Component` or `@InjectPage`, or null
     * @param element the presentation library element associated with this field, or null
     */
    constructor(field: PsiField?, element: PresentationLibraryElement?) {
        this.field = field
        this.tag = null
        this.element = element
    }

    /**
     * Creates an injected element from an XML tag.
     *
     * @param tag the XML tag representing the component in a template, or null
     * @param element the presentation library element associated with this tag, or null
     */
    constructor(tag: XmlTag?, element: PresentationLibraryElement?) {
        this.field = null
        this.tag = tag
        this.element = element
    }

    /**
     * Retrieves all parameters for this injected element.
     *
     * The parameters are extracted from different sources depending on the element type:
     * - For tag-based elements: parameters are taken from the XML tag attributes
     * - For field-based elements: parameters are extracted from the `@Component` annotation's
     *   `parameters` attribute, which should contain key=value pairs
     *
     * @return a map of parameter names to their values, or an empty map if no parameters are found
     */
    val parameters: Map<String, String?>
        get() {
            if (tag != null) {
                return tag.attributes.associate { it.localName to it.value }
            }

            val componentParameters = this.field?.getAnnotation(TapestryConstants.COMPONENT_ANNOTATION)
                ?.attributeValues("parameters")?.ifEmpty { null } ?: return emptyMap()

            return componentParameters
                .map { it.split("=") }
                .filter { it.size == 2 }
                .associate { it[0] to it[1] }
        }

    /**
     * Determines the unique identifier for this injected element.
     *
     * The ID is extracted using the following logic:
     * - For field-based components: returns the ID from the `@Component` annotation's `id` parameter,
     *   or the field name if no explicit ID is specified
     * - For tag-based components: returns the `id` parameter value if present, otherwise uses the
     *   element name if it differs from the tag name (case-insensitive), or falls back to the tag's local name
     *
     * @return the element ID, or null if the element cannot be identified or is not a component
     */
    val elementId: String?
        get() {
            if (element == null || (this.field == null && tag == null)) return null

            if (this.field != null) {
                return if (element is TapestryComponent) getFieldId() else null
            }

            if (tag != null) {
                if (element is TapestryComponent) {
                    parameters["id"]?.let { return it }
                }

                if (!tag.localName.equals(element.name.orEmpty(), ignoreCase = true)) {
                    return element.name
                }

                return tag.localName
            }

            return null
        }

    /**
     * Compares this injected element with another based on their element IDs.
     *
     * @param other the other injected element to compare with
     * @return a negative integer, zero, or a positive integer as this element's ID is less than,
     *         equal to, or greater than the other element's ID
     */
    override fun compareTo(other: InjectedElement): Int = compareValues(elementId, other.elementId)

    /**
     * Checks if this injected element is equal to another object.
     *
     * Two injected elements are considered equal if:
     * - The other object is also an InjectedElement
     * - Their element IDs are equal (compareTo returns 0)
     * - Their presentation library element names are equal
     *
     * @param other the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean =
        other is InjectedElement &&
                compareTo(other) == 0 &&
                element?.name == other.element?.name

    /**
     * Computes the hash code for this injected element based on its element ID.
     *
     * @return the hash code of the element ID, or 0 if the element ID is null
     */
    override fun hashCode(): Int = elementId?.hashCode() ?: 0

    /**
     * Extracts the ID from a field-based injected element.
     *
     * First attempts to retrieve an explicit ID from the `@Component` annotation's `id` parameter.
     * If no explicit ID is found, uses the field name (converted to a proper component ID format).
     *
     * @return the field ID, or null if the field is null
     */
    private fun getFieldId(): String? {
        val id = field?.getAnnotation(TapestryConstants.COMPONENT_ANNOTATION)?.attributeValues("id")

        return if (id != null && id.isNotEmpty()) id[0] else ClassUtils.getName(field?.name ?: return null)
    }
}

/**
 * Represents a template element, which is an injected element associated with its containing template.
 *
 * This class pairs an [InjectedElement] with the name or path of the template file where it is declared,
 * allowing tracking of which template a particular injected element belongs to.
 */
class TemplateElement(
    /** The injected element found in the template, or null. */
    var element: InjectedElement?,
    /** The name or path of the template file containing this element, or null. */
    var template: String?
) : Comparable<TemplateElement> {

    /**
     * Compares this template element with another based on their template names/paths.
     *
     * @param other the other template element to compare with
     * @return a negative integer, zero, or a positive integer as this element's template is less than,
     *         equal to, or greater than the other element's template (lexicographically)
     */
    override fun compareTo(other: TemplateElement): Int = template.orEmpty().compareTo(other.template.orEmpty())

    /**
     * Checks if this template element is equal to another object.
     *
     * Two template elements are considered equal if:
     * - The other object is also a TemplateElement
     * - Their injected elements are equal
     * - Their template names/paths are equal
     *
     * @param other the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean =
        other is TemplateElement && element == other.element && template == other.template

    /**
     * Computes the hash code for this template element based on its template name/path.
     *
     * @return the hash code of the template name/path, or 0 if the template is null
     */
    override fun hashCode(): Int = template?.hashCode() ?: 0
}
