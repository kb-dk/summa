package dk.statsbiblioteket.summa.score.server;

import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.summa.score.api.ClientConnection;
import dk.statsbiblioteket.summa.score.bundle.BundleRepository;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.IOException;
import java.util.List;

/**
 * RMI specialization of the public {@link ScoreConnection} interface.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public interface ScoreRMIConnection extends ScoreConnection, Remote {

    public ConfigurationStorage getConfigurationStorage() throws RemoteException;

    public BundleRepository getRepository() throws RemoteException;

    public ClientConnection getClient(String instanceId) throws RemoteException;

    public void deployClient(Configuration conf) throws RemoteException;

    public void startClient(Configuration conf) throws RemoteException;

    public List<String> getClients() throws RemoteException;

}
