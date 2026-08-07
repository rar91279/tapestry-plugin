package com.github.rar91279.plugin.tapestry.core.events

/**
 * A Tapestry model change listener.
 * Classes that want to be notified of any change in the Tapestry model should implement this interface.
 */
interface TapestryModelChangeListener {

    fun modelChanged()
}
