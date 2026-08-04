package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.wrapper

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Wraps the XML parsing logic of a presentation element documentation descriptor.
 */
class PresentationElementDocumentationWrapper @JvmOverloads constructor(url: URL? = null) {

    private val root: Element? = url?.let {
        DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(it.openStream()).documentElement
    }

    /** The element description, never `null`. */
    val description: String by lazy { normalize(firstTextContent("description")) }

    /** The element examples, never `null`. */
    val examples: String by lazy { firstTextContent("examples") }

    /** The element notes, never `null`. */
    val notes: String by lazy { firstTextContent("notes") }

    /** All documented parameters, keyed by name in document order. Never `null`. */
    val parameters: Map<String, String> by lazy {
        parameterNodes()
            .mapNotNull { parameter ->
                val name = parameter.attributes?.getNamedItem("name")?.textContent ?: return@mapNotNull null
                name to normalize(parameter.textContent)
            }
            .toMap()
    }

    /**
     * @return the description of the given parameter, or an empty string if it isn't documented.
     */
    fun getParameterDescription(name: String): String =
        parameterNodes().firstOrNull { it.attributes?.getNamedItem("name")?.textContent == name }?.textContent ?: ""

    private fun parameterNodes(): Sequence<Node> = nodes("parameter")

    private fun firstTextContent(tagName: String): String = nodes(tagName).firstOrNull()?.textContent ?: ""

    private fun nodes(tagName: String): Sequence<Node> {
        val nodes = root?.getElementsByTagName(tagName) ?: return emptySequence()
        return (0 until nodes.length).asSequence().map { nodes.item(it) }
    }

    private companion object {
        /** Strips the XML's manual line-continuation backslashes and collapses whitespace runs. */
        fun normalize(text: String): String = text.replace(Regex("\\\\\\s"), " ").replace(Regex("\\s+"), " ").trim()
    }
}
