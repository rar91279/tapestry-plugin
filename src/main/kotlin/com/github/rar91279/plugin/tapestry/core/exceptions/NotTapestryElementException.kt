package com.github.rar91279.plugin.tapestry.core.exceptions

/**
 * Thrown when an action that only works on a Tapestry component is executed in a not Tapestry component.
 */
class NotTapestryElementException(message: String) : RuntimeException(message)
