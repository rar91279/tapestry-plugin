package com.github.rar91279.plugin.tapestry.core.util

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.jar.JarFile

/**
 * Lists the classpath entries of a package, whether they live in a directory or inside a jar.
 */
object ClassLocator {

    /**
     * @param packageName the package to scan, e.g. `documentation.core.components`.
     * @return one location per entry found under the package, sub-packages included.
     */
    fun locate(classLoader: ClassLoader, packageName: String): List<ClassLocation> {
        val path = packageName.replace('.', '/')
        val locations = ArrayList<ClassLocation>()

        for (resource in classLoader.getResources(path)) {
            when (resource.protocol.lowercase()) {
                "file" -> locations += fromDirectory(File(resource.file))
                "jar" -> locations += fromJar(resource, path)
                else -> throw IOException("Unknown protocol on class resource: ${resource.toExternalForm()}")
            }
        }

        return locations
    }

    private fun fromDirectory(directory: File): List<ClassLocation> {
        if (!directory.isDirectory) throw IOException("Invalid directory ${directory.absolutePath}")

        return directory.walkTopDown()
            .filter { it.isFile }
            .map { ClassLocation(it.name.substringBeforeLast('.'), it.toURI().toURL()) }
            .toList()
    }

    private fun fromJar(resource: URL, packagePath: String): List<ClassLocation> {
        // Derive the jar file from the URL rather than casting the connection: IntelliJ's
        // PathClassLoader returns its own URLConnection type, not java.net.JarURLConnection.
        val spec = resource.file // e.g. file:/path/plugin.jar!/documentation/core/components
        val jarUri = URI(spec.substringBefore("!/"))

        JarFile(File(jarUri)).use { jar ->
            return jar.entries().asSequence()
                .filter { it.name.startsWith(packagePath) && !it.name.endsWith("/") }
                .map {
                    val className = PathUtils.getLastPathElement(PathUtils.toUnixPath(it.name)).substringBeforeLast('.')
                    ClassLocation(className, URI("jar:$jarUri!/${it.name}").toURL())
                }
                .toList()
        }
    }
}
