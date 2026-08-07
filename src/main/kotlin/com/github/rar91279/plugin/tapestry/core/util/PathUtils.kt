package com.github.rar91279.plugin.tapestry.core.util

import java.io.File

/**
 * Utility methods for path manipulation.
 */
object PathUtils {

    /** The Tapestry path separator character. */
    const val TAPESTRY_PATH_SEPARATOR = "/"

    /** The file path separator character. */
    val SYSTEM_PATH_SEPARATOR: String = File.separator

    /** The unix file path separator character. */
    const val UNIX_PATH_SEPARATOR = "/"

    /** The windows file path separator character. */
    const val WINDOWS_PATH_SEPARATOR = "\\"

    /** The package separator character. */
    const val PACKAGE_SEPARATOR = "."

    /**
     * Transforms a package name into a valid Tapestry path.
     * Example: `admin.login` -> `admin/login`
     *
     * @param includeFinalSeparator if a path separator should be included in the end.
     */
    fun packageIntoPath(packageName: String?, includeFinalSeparator: Boolean): String {
        if (packageName.isNullOrEmpty()) return ""

        return packageName.replace(PACKAGE_SEPARATOR, TAPESTRY_PATH_SEPARATOR) +
                if (includeFinalSeparator) TAPESTRY_PATH_SEPARATOR else ""
    }

    /**
     * Transforms a path into a package.
     * Example: `admin/login` -> `admin.login`
     *
     * @param removeLastElement if the last element of the path should be removed from the resulting package.
     */
    fun pathIntoPackage(path: String?, removeLastElement: Boolean): String {
        if (path.isNullOrEmpty()) return ""

        var result = path.removeSuffix(TAPESTRY_PATH_SEPARATOR).removePrefix(TAPESTRY_PATH_SEPARATOR)

        if (removeLastElement && result.contains(TAPESTRY_PATH_SEPARATOR)) {
            result = result.substringBeforeLast(TAPESTRY_PATH_SEPARATOR)
        }

        return result.replace(TAPESTRY_PATH_SEPARATOR, PACKAGE_SEPARATOR)
    }

    /**
     * Constructs the full package name of a component.
     * Example: `com.myapp.pages` | `admin/Login` -> `com.myapp.pages.admin`
     */
    fun getFullComponentPackage(basePackage: String?, componentName: String?): String {
        if (componentName.isNullOrEmpty() || !componentName.contains(TAPESTRY_PATH_SEPARATOR)) {
            return basePackage ?: ""
        }

        val path = componentName.substringBeforeLast(TAPESTRY_PATH_SEPARATOR)
        return basePackage + PACKAGE_SEPARATOR + path.replace(TAPESTRY_PATH_SEPARATOR, PACKAGE_SEPARATOR)
    }

    /**
     * Computes the last element of a path.
     * Example: `admin/Login` -> `Login`
     */
    fun getLastPathElement(path: String?): String {
        if (path.isNullOrEmpty()) return ""

        return path.substringAfterLast(TAPESTRY_PATH_SEPARATOR)
    }

    /**
     * Computes the first element of a path.
     * Example: `admin/Login` -> `admin`
     */
    fun getFirstPathElement(path: String?): String {
        if (path.isNullOrEmpty()) return ""
        if (!path.contains(TAPESTRY_PATH_SEPARATOR)) return path

        return path.removePrefix(TAPESTRY_PATH_SEPARATOR).substringBefore(TAPESTRY_PATH_SEPARATOR)
    }

    /**
     * Removes the last path element.
     * Example: `admin/Login` -> `admin`
     *
     * @param removeIfOnlyOneElement remove the last element even if it's the only element in the path.
     */
    fun removeLastFilePathElement(path: String?, removeIfOnlyOneElement: Boolean): String {
        if (path.isNullOrEmpty()) return ""

        val separator = findSeparator(path) ?: return if (removeIfOnlyOneElement) "" else path

        return path.substringBeforeLast(separator)
    }

    /**
     * Computes the component file name from the component name.
     * Example: `admin/Login` -> `Login`
     */
    fun getComponentFileName(componentName: String?): String = getLastPathElement(componentName)

    /**
     * Returns a given path in UNIX format.
     */
    fun toUnixPath(path: String?): String? = path?.replace(File.separatorChar, '/')

    private fun findSeparator(path: String): String? = when {
        path.contains(WINDOWS_PATH_SEPARATOR) -> WINDOWS_PATH_SEPARATOR
        path.contains(UNIX_PATH_SEPARATOR) -> UNIX_PATH_SEPARATOR
        else -> null
    }
}
