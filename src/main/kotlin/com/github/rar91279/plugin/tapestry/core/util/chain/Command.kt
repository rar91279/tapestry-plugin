package com.github.rar91279.plugin.tapestry.core.util.chain

/**
 * A unit of work in a [Chain].
 */
interface Command {

    /**
     * Executes this command.
     *
     * @param context the shared execution context.
     * @return `true` if processing is complete and the chain should stop, `false` to continue.
     */
    @Throws(Exception::class)
    fun execute(context: Context): Boolean
}
