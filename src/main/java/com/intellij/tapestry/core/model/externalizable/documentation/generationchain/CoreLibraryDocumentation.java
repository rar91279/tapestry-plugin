package com.intellij.tapestry.core.model.externalizable.documentation.generationchain;

import com.intellij.tapestry.core.model.externalizable.documentation.wrapper.PresentationElementDocumentationWrapper;
import com.intellij.tapestry.core.util.ClassLocator;
import com.intellij.tapestry.core.util.VelocityProcessor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders the documentation page for a bundled core-library element (component, page or mixin),
 * driven purely by its shipped {@code documentation/core/<type>/<name>.xml} descriptor.
 */
public final class CoreLibraryDocumentation {

    private static final String TEMPLATE = "/documentation/core-element.vm";

    private CoreLibraryDocumentation() {
    }

    /**
     * @param type the element folder: {@code components}, {@code pages} or {@code mixins}.
     * @param name the element name (XML file name without extension).
     * @return the rendered HTML, or {@code null} if no descriptor exists for the element.
     */
    public static String render(String type, String name) throws Exception {
        URL xml = CoreLibraryDocumentation.class.getResource("/documentation/core/" + type + "/" + name + ".xml");
        if (xml == null)
            return null;

        PresentationElementDocumentationWrapper documentation = new PresentationElementDocumentationWrapper(xml);

        Map<String, Object> context = AbstractDocumentationGenerator.baseContext();
        context.put("name", name);
        context.put("documentation", documentation);

        return VelocityProcessor.processClasspathTemplate(TEMPLATE, context);
    }

    /** Renders the core-library index: the bundled pages, components and mixins as clickable entries. */
    public static String renderIndex() {
        List<NavPageDocumentation.Section> sections = new ArrayList<>();
        sections.add(indexSection("Pages", "pages"));
        sections.add(indexSection("Components", "components"));
        sections.add(indexSection("Mixins", "mixins"));
        return NavPageDocumentation.render("Core Library", sections);
    }

    private static NavPageDocumentation.Section indexSection(String title, String kind) {
        List<NavPageDocumentation.Entry> entries = new ArrayList<>();
        try {
            ClassLocator locator = new ClassLocator(CoreLibraryDocumentation.class.getClassLoader(),
                    "documentation.core." + kind);
            for (ClassLocator.ClassLocation location : locator.getAllClassLocations()) {
                if (!location.getUrl().toExternalForm().endsWith(".xml"))
                    continue;

                String name = location.getClassName();
                String description = "";
                try {
                    description = new PresentationElementDocumentationWrapper(location.getUrl()).getDescription();
                } catch (Exception ignore) {
                    // no descriptor detail — list the name alone
                }
                entries.add(new NavPageDocumentation.Entry(name, "core/" + kind + "/" + name,
                        NavPageDocumentation.summary(description)));
            }
        } catch (Exception ex) {
            // an unreadable core index degrades to an empty section rather than a broken page
        }
        entries.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getLabel(), b.getLabel()));
        return new NavPageDocumentation.Section(title, entries);
    }
}
