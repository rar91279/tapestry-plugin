package com.github.rar91279.plugin.tapestry.core.events

import java.util.EventListener

/**
 * A Tapestry model change listener.
 * Classes that want to be notified of any change in the Tapestry model should implement this interface.
 */
interface TapestryModelChangeListener : EventListener {

    /**
     * Called when the Tapestry model has changed.
     * Implementors should update their state or UI in response to model changes.
     */
    fun modelChanged()
}
