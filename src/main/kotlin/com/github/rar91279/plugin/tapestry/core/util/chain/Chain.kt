package com.github.rar91279.plugin.tapestry.core.util.chain

/**
 * An ordered sequence of [Command]s that is itself a [Command].
 */
interface Chain : Command {

    /**
     * Appends a command to this chain.
     */
    fun addCommand(command: Command)
}
