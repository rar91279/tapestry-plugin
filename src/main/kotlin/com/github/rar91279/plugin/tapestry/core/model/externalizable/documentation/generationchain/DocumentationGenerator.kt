package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain

import com.intellij.openapi.diagnostic.Logger
import com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.Home
import com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.wrapper.PresentationElementDocumentationWrapper
import com.github.rar91279.plugin.tapestry.core.model.presentation.Mixin
import com.github.rar91279.plugin.tapestry.core.model.presentation.Page
import com.github.rar91279.plugin.tapestry.core.model.presentation.PresentationLibraryElement
import com.github.rar91279.plugin.tapestry.core.model.presentation.TapestryComponent
import com.github.rar91279.plugin.tapestry.core.util.ClassLocator
import com.github.rar91279.plugin.tapestry.core.util.VelocityProcessor

/**
 * Renders the "Live Documentation" HTML for a documentable element. Dispatches by element type to the
 * matching Velocity template; presentation elements (component/page/mixin) share one template and
 * differ only by their icon and the descriptor sub-folder.
 */
object DocumentationGenerator {

    private val logger = Logger.getInstance(DocumentationGenerator::class.java)

    private const val PRESENTATION_TEMPLATE = "/documentation/presentation-element.vm"
    private const val HOME_TEMPLATE = "/documentation/home.vm"
    // Backed by intellij.platform.util.ui.jar (core platform, always on the classpath) — the same
    // resource paths AllIcons.Nodes.{Class,Method,Parameter} load internally.
    private const val COMPONENT_ICON = "/expui/nodes/class.svg"
    private const val PAGE_ICON = "/expui/nodes/parameter.svg"
    private const val MIXIN_ICON = "/expui/nodes/method.svg"
    private val HOME_ELEMENT_TYPES = arrayOf("components", "pages", "mixins")

    /** @return the rendered HTML, or `null` if the element type is not documentable. */
    fun generate(element: Any): String? = when (element) {
        is Home -> renderHome(element)
        is TapestryComponent -> renderPresentationElement(element, "components", COMPONENT_ICON)
        is Page -> renderPresentationElement(element, "pages", PAGE_ICON)
        is Mixin -> renderPresentationElement(element, "mixins", MIXIN_ICON)
        else -> null
    }

    private fun renderPresentationElement(element: PresentationLibraryElement, kind: String, iconPath: String): String {
        if (element.elementClass?.containingFile == null)
            logger.error("Couldn't find file for class \"${element.elementClass?.qualifiedName}\"")

        val context = DocAssets.baseContext()
        context["element"] = element
        context["icon"] = javaClass.getResource(iconPath)
        context["documentation"] = try {
            PresentationElementDocumentationWrapper(descriptorUrl(element.library?.id, kind, element.name))
        } catch (ex: Exception) {
            PresentationElementDocumentationWrapper()
        }
        return VelocityProcessor.processClasspathTemplate(PRESENTATION_TEMPLATE, context)
    }

    private fun descriptorUrl(library: String?, kind: String, name: String?) =
        javaClass.getResource("/documentation/$library/$kind/$name.xml")

    private fun renderHome(home: Home): String {
        val context = DocAssets.baseContext()
        context["modules"] = home.modules

        for (elementType in HOME_ELEMENT_TYPES) {
            val descriptors = HashMap<String, String>()
            val resources = try {
                ClassLocator.locate(javaClass.classLoader, "documentation.core.$elementType")
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }
            for (resource in resources) {
                if (!resource.url.toExternalForm().endsWith(".xml")) continue
                val description = PresentationElementDocumentationWrapper(resource.url).description
                descriptors[resource.className] = NavPageDocumentation.summary(description)
            }
            context[elementType] = descriptors
        }
        return VelocityProcessor.processClasspathTemplate(HOME_TEMPLATE, context)
    }
}
