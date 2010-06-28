package dk.statsbiblioteket.summa.common.shell.notifications;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * @author Henrik <mailto:hbk@statsbiblioteket.dk>
 * @since Jun 28, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk")
public class ClearNotification extends Notification {
  public ClearNotification(Command cmd) {
        super(cmd);
    }
}
