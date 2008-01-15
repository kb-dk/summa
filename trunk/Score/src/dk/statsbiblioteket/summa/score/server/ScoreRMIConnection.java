package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.score.api.ScoreConnection;

import java.rmi.Remote;

/**
 * RMI specialization of the public {@link ScoreConnection} interface.
 */
public interface ScoreRMIConnection extends ScoreConnection, Remote {
}
