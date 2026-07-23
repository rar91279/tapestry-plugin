package com.intellij.tapestry.core.util.chain;

/**
 * A unit of work in a {@link Chain}.
 */
public interface Command {

    /**
     * Executes this command.
     *
     * @param context the shared execution context.
     * @return {@code true} if processing is complete and the chain should stop, {@code false} to continue.
     * @throws Exception if an error occurs.
     */
    boolean execute(Context context) throws Exception;
}
