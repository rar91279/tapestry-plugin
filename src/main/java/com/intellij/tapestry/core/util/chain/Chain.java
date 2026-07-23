package com.intellij.tapestry.core.util.chain;

/**
 * An ordered sequence of {@link Command}s that is itself a {@link Command}.
 */
public interface Chain extends Command {

    /**
     * Appends a command to this chain.
     *
     * @param command the command to add.
     */
    void addCommand(Command command);
}
