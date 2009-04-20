package dk.statsbiblioteket.summa.common.shell.notifications;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.Core;

/**
 * Notify the shell {@link Core} that we want a backtrace printed
 */
public class TraceNotification extends Notification {

    public TraceNotification(Command cmd) {
        super(cmd);
    }
}



