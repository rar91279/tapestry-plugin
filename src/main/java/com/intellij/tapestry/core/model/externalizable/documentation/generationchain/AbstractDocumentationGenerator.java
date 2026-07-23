package com.intellij.tapestry.core.model.externalizable.documentation.generationchain;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all documentation generator commands.
 */
public abstract class AbstractDocumentationGenerator implements Command {

    private static final Logger _logger = Logger.getInstance(AbstractDocumentationGenerator.class);

    private static final String BASE_PATH = "/documentation/";
    // Inline as data: URIs — the JCEF renderer can't fetch jar: classpath URLs.
    private static final String LOGO = dataUri("/documentation/tapestry-logo.svg", "image/svg+xml");
    private static final String STYLE = dataUri("/documentation/style.css", "text/css");

    @Override
    public boolean execute(Context context) throws Exception {
        return context instanceof DocumentationGenerationContext;
    }

    public URL getDocumentationURL(String library, String middlePath, String name) {
        return getClass().getResource(BASE_PATH + library + "/" + middlePath + "/" + name + ".xml");
    }

    Map<String, Object> buildVelocityContext() {
        return baseContext();
    }

    /** The logo as an inlined {@code data:} URI (for embedding outside the Velocity templates). */
    public static String logo() {
        return LOGO;
    }

    /** Shared context with the inlined stylesheet and logo. */
    static Map<String, Object> baseContext() {
        Map<String, Object> velocityContext = new HashMap<>();
        velocityContext.put("style", STYLE);
        velocityContext.put("logo", LOGO);
        return velocityContext;
    }

    private static String dataUri(String resource, String mimeType) {
        try (InputStream in = AbstractDocumentationGenerator.class.getResourceAsStream(resource)) {
            if (in == null) {
                _logger.warn("Documentation resource not found: " + resource);
                return "";
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (Exception ex) {
            _logger.error(ex);
            return "";
        }
    }
}
