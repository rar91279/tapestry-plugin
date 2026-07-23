package com.intellij.tapestry.core.util.chain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base {@link Chain} implementation: runs its commands in order and stops at the
 * first one that returns {@code true}.
 */
public class ChainBase implements Chain {

    private final List<Command> commands = new ArrayList<>();

    public ChainBase() {
    }

    public ChainBase(Command[] commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    @Override
    public void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public boolean execute(Context context) throws Exception {
        for (Command command : commands) {
            if (command.execute(context)) {
                return true;
            }
        }
        return false;
    }
}
