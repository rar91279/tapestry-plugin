package com.github.rar91279.plugin.tapestry.core.model.externalizable.documentation.generationchain

import com.github.rar91279.plugin.tapestry.core.resource.IResource

/**
 * Velocity helper for building navigation tokens consumed by the doc tab's `tapestryNav` bridge.
 * Exposed to templates as `$nav`.
 */
object DocNav {

    /** `file/<path>` token opening the resource's file, or `""` if it has no local file. */
    fun fileToken(resource: IResource?): String {
        val file = resource?.file ?: return ""
        // Forward slashes accepted by the local file system on all platforms; js() handles quoting.
        return "file/" + file.path.replace('\\', '/')
    }

    /**
     * Escapes a navigation token for embedding inside a single-quoted JS string literal that itself
     * sits inside a double-quoted HTML `onclick` attribute. Without this an apostrophe in a module
     * name or file path (e.g. `module/John's App`) terminates the JS string and breaks the
     * `tapestryNav('...')` call. HTML entities are decoded before the JS runs, so the token reaches
     * the bridge unchanged.
     */
    fun js(token: String?): String {
        if (token == null) return ""
        val sb = StringBuilder(token.length + 8)
        for (c in token) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\'' -> sb.append("\\'")
                '&' -> sb.append("&amp;")
                '"' -> sb.append("&quot;")
                '<' -> sb.append("&lt;")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
