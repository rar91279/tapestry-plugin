package com.intellij.tapestry.core.model.externalizable.documentation.generationchain

import com.intellij.openapi.diagnostic.Logger
import java.util.Base64

/**
 * Shared assets for the documentation generators: the inlined stylesheet and logo, exposed through a
 * base Velocity context.
 */
object DocAssets {

    private val logger = Logger.getInstance(DocAssets::class.java)

    // Inline as data: URIs — the JCEF renderer can't fetch jar: classpath URLs.
    private val LOGO = dataUri("/documentation/tapestry-docs-logo.svg", "image/svg+xml")
    private val STYLE = dataUri("/documentation/style.css", "text/css")

    /** The logo as an inlined `data:` URI (for embedding outside the Velocity templates). */
    fun logo(): String = LOGO

    /** Shared context with the inlined stylesheet and logo. */
    internal fun baseContext(): MutableMap<String, Any> = hashMapOf(
        "style" to STYLE,
        "logo" to LOGO,
        "nav" to DocNav,
    )

    private fun dataUri(resource: String, mimeType: String): String =
        try {
            javaClass.getResourceAsStream(resource)?.use { input ->
                "data:$mimeType;base64," + Base64.getEncoder().encodeToString(input.readAllBytes())
            } ?: run {
                logger.warn("Documentation resource not found: $resource")
                ""
            }
        } catch (ex: Exception) {
            logger.error(ex)
            ""
        }
}
