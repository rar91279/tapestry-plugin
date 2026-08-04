package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain

import com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.wrapper.PresentationElementDocumentationWrapper
import com.github.rar91279.plugin.tapestry.core.util.ClassLocator
import com.github.rar91279.plugin.tapestry.core.util.VelocityProcessor

/**
 * Renders the documentation page for a bundled core-library element (component, page or mixin),
 * driven purely by its shipped `documentation/core/<type>/<name>.xml` descriptor.
 */
object CoreLibraryDocumentation {

    private const val ELEMENT_TEMPLATE = "/documentation/core-element.vm"

    /**
     * @param type the element folder: `components`, `pages` or `mixins`.
     * @param name the element name (XML file name without extension).
     * @return the rendered HTML, or `null` if no descriptor exists for the element.
     */
    fun render(type: String, name: String): String? {
        val xml = CoreLibraryDocumentation::class.java.getResource("/documentation/core/$type/$name.xml")
            ?: return null

        val context = DocAssets.baseContext()
        context["name"] = name
        context["documentation"] = PresentationElementDocumentationWrapper(xml)

        return VelocityProcessor.processClasspathTemplate(ELEMENT_TEMPLATE, context)
    }

    /** Renders the core-library index: the bundled pages, components and mixins as clickable entries. */
    fun renderIndex(): String {
        val sections = listOf(
            indexSection("Pages", "pages"),
            indexSection("Components", "components"),
            indexSection("Mixins", "mixins"))
        return NavPageDocumentation.render("Core Library", sections)
    }

    private fun indexSection(title: String, kind: String): NavPageDocumentation.Section {
        val entries = ArrayList<NavPageDocumentation.Entry>()
        try {
            val locations = ClassLocator.locate(CoreLibraryDocumentation::class.java.classLoader, "documentation.core.$kind")
            for (location in locations) {
                if (!location.url.toExternalForm().endsWith(".xml")) continue

                val name = location.className
                val description = try {
                    PresentationElementDocumentationWrapper(location.url).description
                } catch (ignore: Exception) {
                    // no descriptor detail — list the name alone
                    ""
                }
                entries.add(NavPageDocumentation.Entry(name, "core/$kind/$name", NavPageDocumentation.summary(description)))
            }
        } catch (ex: Exception) {
            // an unreadable core index degrades to an empty section rather than a broken page
        }
        entries.sortWith { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label) }
        return NavPageDocumentation.Section(title, entries)
    }
}
