package dk.statsbiblioteket.summa.common.shell.notifications;

/**
 * Emitted when the user enters a malformed command line. This notification
 * does not map to a valid command hence {@link #getCommand} will return
 * {@code null}. 
 *
 */
public class SyntaxErrorNotification extends Notification {

    public SyntaxErrorNotification(String msg) {
        super(msg);
    }
}
