package com.intellij.tapestry.core.model.externalizable.documentation.generationchain;

import com.intellij.tapestry.core.util.VelocityProcessor;

import java.util.List;
import java.util.Map;

/**
 * Renders a navigation page: a titled list of sections, each holding clickable entries. Used for the
 * Home page (modules + core), module detail pages and the core-library index.
 */
public final class NavPageDocumentation {

    private static final String TEMPLATE = "/documentation/container.vm";

    private NavPageDocumentation() {
    }

    public static String render(String title, List<Section> sections) {
        return render(title, "", "", sections);
    }

    public static String render(String title, String subtitle, List<Section> sections) {
        return render(title, subtitle, "", sections);
    }

    /**
     * {@code subtitle} is shown right-aligned in the page header (e.g. Maven coordinates). If
     * {@code subtitleToken} is non-empty the subtitle is a clickable navigation link.
     */
    public static String render(String title, String subtitle, String subtitleToken, List<Section> sections) {
        Map<String, Object> context = AbstractDocumentationGenerator.baseContext();
        context.put("title", title == null ? "" : title);
        context.put("subtitle", subtitle == null ? "" : subtitle);
        context.put("subtitleToken", subtitleToken == null ? "" : subtitleToken);
        context.put("sections", sections);
        return VelocityProcessor.processClasspathTemplate(TEMPLATE, context);
    }

    /** Collapses markup/whitespace and trims to a one-line summary. */
    public static String summary(String text) {
        if (text == null)
            return "";
        String clean = text.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (clean.length() <= 120)
            return clean;
        int cut = clean.lastIndexOf(' ', 120);
        return clean.substring(0, cut < 0 ? 120 : cut) + "…";
    }

    /** A titled group of entries. */
    public static class Section {

        private final String _name;
        private final List<Entry> _entries;

        public Section(String name, List<Entry> entries) {
            _name = name;
            _entries = entries;
        }

        public String getName() {
            return _name;
        }

        public List<Entry> getEntries() {
            return _entries;
        }
    }

    /**
     * A list entry: a label plus a navigation token consumed by the {@code tapestryNav} bridge (empty
     * token → not clickable), an optional description, and an optional badge.
     */
    public static class Entry {

        private final String _label;
        private final String _token;
        private final String _description;
        private final String _badge;
        private final String _descriptionToken;

        public Entry(String label, String token, String description) {
            this(label, token, description, "", "");
        }

        public Entry(String label, String token, String description, String badge) {
            this(label, token, description, badge, "");
        }

        /** {@code descriptionToken}, if non-empty, makes the description a clickable navigation link. */
        public Entry(String label, String token, String description, String badge, String descriptionToken) {
            _label = label;
            _token = token;
            _description = description;
            _badge = badge;
            _descriptionToken = descriptionToken;
        }

        public String getDescriptionToken() {
            return _descriptionToken;
        }

        public String getLabel() {
            return _label;
        }

        public String getToken() {
            return _token;
        }

        public String getDescription() {
            return _description;
        }

        public String getBadge() {
            return _badge;
        }
    }
}
