package com.intellij.tapestry.core.model.externalizable.documentation.generationchain;

import com.intellij.tapestry.core.resource.IResource;

/**
 * Velocity helper for building navigation tokens consumed by the doc tab's {@code tapestryNav} bridge.
 * Exposed to templates as {@code $nav}.
 */
public final class DocNav {

    public static final DocNav INSTANCE = new DocNav();

    private DocNav() {
    }

    /** {@code file/<path>} token opening the resource's file, or {@code ""} if it has no local file. */
    public String fileToken(IResource resource) {
        if (resource == null || resource.getFile() == null)
            return "";
        // Forward slashes accepted by the local file system on all platforms; js() handles quoting.
        return "file/" + resource.getFile().getPath().replace('\\', '/');
    }

    /**
     * Escapes a navigation token for embedding inside a single-quoted JS string literal that itself
     * sits inside a double-quoted HTML {@code onclick} attribute. Without this an apostrophe in a
     * module name or file path (e.g. {@code module/John's App}) terminates the JS string and breaks
     * the {@code tapestryNav('...')} call. HTML entities are decoded before the JS runs, so the token
     * reaches the bridge unchanged.
     */
    public String js(String token) {
        if (token == null)
            return "";
        StringBuilder sb = new StringBuilder(token.length() + 8);
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '<': sb.append("&lt;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
