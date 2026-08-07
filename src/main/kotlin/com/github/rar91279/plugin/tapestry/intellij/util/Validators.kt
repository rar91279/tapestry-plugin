package com.github.rar91279.plugin.tapestry.intellij.util

/**
 * Utility validators.
 */
object Validators {

    private val PACKAGE_NAME = Regex("[a-zA-Z_\$][\\w\$]*(?:\\.[a-zA-Z_\$][\\w\$]*)*")
    private val COMPONENT_NAME = Regex("[a-zA-Z_\$][\\w\$]*(?:/[a-zA-Z_\$][\\w\$]*)*")

    /**
     * @return `true` if the given string is a valid package name, `false` otherwise.
     */
    fun isValidPackageName(packageName: String?): Boolean = packageName != null && PACKAGE_NAME.matches(packageName)

    /**
     * @return `true` if the given string is a valid component name, `false` otherwise.
     */
    fun isValidComponentName(componentName: String?): Boolean = componentName != null && COMPONENT_NAME.matches(componentName)
}
