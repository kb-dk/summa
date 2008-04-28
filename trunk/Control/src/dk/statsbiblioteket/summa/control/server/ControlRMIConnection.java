package dk.statsbiblioteket.summa.control.server;

import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI specialization of the public {@link ControlConnection} interface.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public interface ControlRMIConnection extends ControlConnection, Remote {

    public ClientConnection getClient(String instanceId) throws RemoteException;

    public void deployClient(Configuration conf) throws RemoteException;

    public void startClient(Configuration conf) throws RemoteException;

    public List<String> getClients() throws RemoteException;

    public List<String> getBundles() throws RemoteException;

    public Configuration getDeployConfiguration (String instanceId)
                                                        throws RemoteException;

}
