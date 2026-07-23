package com.intellij.tapestry.core.model.externalizable.documentation.generationchain;

import com.intellij.tapestry.core.model.externalizable.documentation.Home;
import com.intellij.tapestry.core.util.VelocityProcessor;

import java.util.Map;

/**
 * Renders the documentation page for a single Tapestry IoC service.
 */
public final class ServiceDocumentation {

    private static final String TEMPLATE = "/documentation/service.vm";

    private ServiceDocumentation() {
    }

    public static String render(Home.ServiceDoc service) {
        Map<String, Object> context = AbstractDocumentationGenerator.baseContext();
        context.put("service", service);
        return VelocityProcessor.processClasspathTemplate(TEMPLATE, context);
    }
}
