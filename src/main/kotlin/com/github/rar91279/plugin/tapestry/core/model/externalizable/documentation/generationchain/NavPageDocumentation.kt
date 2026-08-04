package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain

import com.github.rar91279.plugin.tapestry.core.util.VelocityProcessor

/**
 * Renders a navigation page: a titled list of sections, each holding clickable entries. Used for the
 * Home page (modules + core), module detail pages and the core-library index.
 */
object NavPageDocumentation {

    private const val TEMPLATE = "/documentation/container.vm"

    fun render(title: String?, sections: List<Section>): String =
        render(title, "", "", sections)

    fun render(title: String?, subtitle: String?, sections: List<Section>): String =
        render(title, subtitle, "", sections)

    /**
     * [subtitle] is shown right-aligned in the page header (e.g. Maven coordinates). If
     * [subtitleToken] is non-empty the subtitle is a clickable navigation link.
     */
    fun render(title: String?, subtitle: String?, subtitleToken: String?, sections: List<Section>): String {
        val context = DocAssets.baseContext()
        context["title"] = title ?: ""
        context["subtitle"] = subtitle ?: ""
        context["subtitleToken"] = subtitleToken ?: ""
        context["sections"] = sections
        return VelocityProcessor.processClasspathTemplate(TEMPLATE, context)
    }

    /** Collapses markup/whitespace and trims to a one-line summary. */
    fun summary(text: String?): String {
        if (text == null) return ""
        val clean = text.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
        if (clean.length <= 120) return clean
        val cut = clean.lastIndexOf(' ', 120)
        return clean.substring(0, if (cut < 0) 120 else cut) + "…"
    }

    /** A titled group of entries. */
    class Section(val name: String, val entries: List<Entry>)

    /**
     * A list entry: a label plus a navigation token consumed by the `tapestryNav` bridge (empty
     * token → not clickable), an optional description, and an optional badge. [descriptionToken],
     * if non-empty, makes the description a clickable navigation link.
     */
    class Entry(
        val label: String,
        val token: String,
        val description: String,
        val badge: String = "",
        val descriptionToken: String = "",
    )
}
