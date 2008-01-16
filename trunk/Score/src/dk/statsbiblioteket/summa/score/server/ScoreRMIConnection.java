package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;

/**
 * RMI specialization of the public {@link ScoreConnection} interface.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public interface ScoreRMIConnection extends ScoreConnection, Remote {
}
