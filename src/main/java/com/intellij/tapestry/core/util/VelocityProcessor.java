package com.intellij.tapestry.core.util;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeInstance;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Processes velocity templates.
 */
public abstract class VelocityProcessor {

    /**
     * Processes a velocity template from the classpath.
     *
     * @param templatePath the path to the template.
     * @param context      the context.
     * @return the processed template.
     * @throws RuntimeException if an error occurs processing the template.
     */
    public static String processClasspathTemplate(String templatePath, Map<String, Object> context) throws RuntimeException {
        // Read the template via the plugin classloader rather than Velocity's ClasspathResourceLoader:
        // Velocity is provided by the platform, so its own classloader can't see plugin resources.
        String path = templatePath.startsWith("/") ? templatePath.substring(1) : templatePath;
        try (InputStream in = VelocityProcessor.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Template not found on classpath: " + path);
            }

            RuntimeInstance ri = new RuntimeInstance();
            ri.init();

            VelocityContext velocityContext = new VelocityContext(context);
            StringWriter text = new StringWriter();
            ri.evaluate(velocityContext, text, templatePath, new InputStreamReader(in, StandardCharsets.UTF_8));

            return text.toString();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
