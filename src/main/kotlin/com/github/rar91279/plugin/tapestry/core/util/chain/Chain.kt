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

/**
 * Marker interface for a chain execution context. Implementations are typically
 * backed by a [Map] and carry state between [Command]s.
 */
interface Context

/**
 * An ordered sequence of [Command]s that is itself a [Command].
 */
interface Chain : Command {

    /**
     * Appends a command to this chain.
     */
    fun addCommand(command: Command)
}

/**
 * Base [Chain] implementation: runs its commands in order and stops at the
 * first one that returns `true`.
 */
open class ChainBase() : Chain {

    private val commands = mutableListOf<Command>()

    constructor(commands: Array<Command>) : this() {
        this.commands.addAll(commands)
    }

    override fun addCommand(command: Command) {
        commands.add(command)
    }

    @Throws(Exception::class)
    override fun execute(context: Context): Boolean = commands.any { it.execute(context) }
}
