package com.github.rar91279.plugin.tapestry.core.util

import org.apache.velocity.VelocityContext
import org.apache.velocity.runtime.RuntimeInstance
import java.io.InputStreamReader
import java.io.StringWriter

/**
 * Processes velocity templates.
 */
object VelocityProcessor {

    /**
     * Processes a velocity template from the classpath.
     *
     * @throws RuntimeException if an error occurs processing the template.
     */
    fun processClasspathTemplate(templatePath: String, context: Map<String, Any>): String {
        // Read the template via the plugin classloader rather than Velocity's ClasspathResourceLoader:
        // Velocity is provided by the platform, so its own classloader can't see plugin resources.
        val path = templatePath.removePrefix("/")
        val stream = VelocityProcessor::class.java.classLoader.getResourceAsStream(path)
            ?: throw RuntimeException("Template not found on classpath: $path")

        try {
            stream.use { input ->
                val runtime = RuntimeInstance()
                runtime.init()

                val text = StringWriter()
                runtime.evaluate(VelocityContext(context), text, templatePath, InputStreamReader(input, Charsets.UTF_8))

                return text.toString()
            }
        } catch (ex: RuntimeException) {
            throw ex
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }
}
