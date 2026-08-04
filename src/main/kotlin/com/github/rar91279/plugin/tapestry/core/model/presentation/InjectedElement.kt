package com.github.rar91279.plugin.tapestry.core.model.presentation

import com.intellij.openapi.util.text.StringUtil
import com.github.rar91279.plugin.tapestry.core.TapestryConstants
import com.github.rar91279.plugin.tapestry.core.java.IJavaField
import com.github.rar91279.plugin.tapestry.core.resource.xml.XmlTag
import com.github.rar91279.plugin.tapestry.core.util.ClassUtils

/**
 * An injected element: either a field annotated with `@Component`/`@InjectPage`, or a component tag
 * found in a template.
 */
class InjectedElement : Comparable<InjectedElement> {

    val field: IJavaField?
    val tag: XmlTag?
    val element: PresentationLibraryElement?

    constructor(field: IJavaField?, element: PresentationLibraryElement?) {
        this.field = field
        this.tag = null
        this.element = element
    }

    constructor(tag: XmlTag?, element: PresentationLibraryElement?) {
        this.field = null
        this.tag = tag
        this.element = element
    }

    /**
     * Finds all the injected element parameters, taken from the tag attributes or from the field annotation.
     */
    val parameters: Map<String, String?>
        get() {
            if (tag != null) {
                return tag.attributes.associate { it.localName.orEmpty() to it.value }
            }

            val componentParameters = this.field?.annotations?.get(TapestryConstants.COMPONENT_ANNOTATION)
                ?.parameters?.get("parameters") ?: return emptyMap()

            return componentParameters
                .map { it.split("=") }
                .filter { it.size == 2 }
                .associate { it[0] to it[1] }
        }

    /**
     * The injected element id, taken either from the field name itself or from an annotated value.
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

                if (StringUtil.toUpperCase(tag.localName ?: "") != StringUtil.toUpperCase(element.name ?: "")) {
                    return element.name
                }

                return tag.localName
            }

            return null
        }

    override fun compareTo(other: InjectedElement): Int = compareIds(elementId, other.elementId)

    override fun equals(other: Any?): Boolean =
        other is InjectedElement &&
                compareTo(other) == 0 &&
                element?.name == other.element?.name

    override fun hashCode(): Int = elementId?.hashCode() ?: 0

    private fun getFieldId(): String? {
        val id = field?.annotations?.get(TapestryConstants.COMPONENT_ANNOTATION)?.parameters?.get("id")

        return if (id != null && id.isNotEmpty()) id[0] else ClassUtils.getName(field?.name ?: return null)
    }

    private companion object {

        fun compareIds(id: String?, otherId: String?): Int = when {
            id != null && otherId != null -> id.compareTo(otherId)
            id == null && otherId == null -> 0
            id == null -> -1
            else -> 1
        }
    }
}

/**
 * A template element: an injected element together with the template it was declared in.
 */
class TemplateElement(var element: InjectedElement?, var template: String?) : Comparable<TemplateElement> {

    override fun compareTo(other: TemplateElement): Int = template.orEmpty().compareTo(other.template.orEmpty())

    override fun equals(other: Any?): Boolean =
        other is TemplateElement && element == other.element && template == other.template

    override fun hashCode(): Int = template?.hashCode() ?: 0
}
