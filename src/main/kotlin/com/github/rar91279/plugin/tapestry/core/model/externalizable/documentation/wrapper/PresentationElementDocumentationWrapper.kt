package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.wrapper

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Wraps the XML parsing logic of a presentation element documentation descriptor.
 *
 * @param url the URL pointing to the XML documentation descriptor, or `null` if no documentation is available
 */
class PresentationElementDocumentationWrapper @JvmOverloads constructor(url: URL? = null) {

    /**
     * The root XML element of the documentation descriptor.
     * Parsed from the provided URL, or `null` if no URL was given or parsing failed.
     */
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

    /**
     * Returns a sequence of all parameter nodes in the documentation.
     *
     * @return sequence of parameter nodes
     */
    private fun parameterNodes(): Sequence<Node> = nodes("parameter")

    /**
     * Returns the text content of the first node with the given tag name.
     *
     * @param tagName the XML tag name to search for
     * @return the text content of the first matching node, or an empty string if not found
     */
    private fun firstTextContent(tagName: String): String = nodes(tagName).firstOrNull()?.textContent ?: ""

    /**
     * Returns a sequence of all nodes with the given tag name.
     *
     * @param tagName the XML tag name to search for
     * @return sequence of matching nodes, or an empty sequence if the root is `null` or no matches found
     */
    private fun nodes(tagName: String): Sequence<Node> {
        val nodes = root?.getElementsByTagName(tagName) ?: return emptySequence()
        return (0 until nodes.length).asSequence().map { nodes.item(it) }
    }

    /**
     * Normalizes XML text content by stripping manual line-continuation backslashes and collapsing whitespace runs.
     *
     * @param text the raw text content from XML
     * @return normalized text with collapsed whitespace
     */
    private fun normalize(text: String): String = text.replace(Regex("\\\\\\s"), " ").replace(Regex("\\s+"), " ").trim()
}
