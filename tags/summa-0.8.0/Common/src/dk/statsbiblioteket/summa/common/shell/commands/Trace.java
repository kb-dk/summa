package dk.statsbiblioteket.summa.common.shell.commands;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.notifications.TraceNotification;
import dk.statsbiblioteket.summa.common.shell.Core;

/**
 * Command used to display the last stack trace recorded by the shell
 * {@link Core}.
 */
public class Trace extends Command {

    public Trace() {
        super("trace", "Print the last recorded stack trace");
    }

    public void invoke(ShellContext ctx) throws Exception {
        throw new TraceNotification (this);
    }
}
