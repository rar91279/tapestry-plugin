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
        // Forward slashes: JS-string safe and accepted by the local file system on all platforms.
        return "file/" + resource.getFile().getPath().replace('\\', '/');
    }
}
