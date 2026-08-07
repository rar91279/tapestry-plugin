package com.github.rar91279.plugin.tapestry.core.util.chain

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
