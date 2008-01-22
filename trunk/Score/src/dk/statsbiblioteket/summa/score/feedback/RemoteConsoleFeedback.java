package dk.statsbiblioteket.summa.score.feedback;

import dk.statsbiblioteket.summa.score.api.Message;
import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;

import java.util.List;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;

/**
 *
 */
public class RemoteConsoleFeedback extends UnicastRemoteObject
                                   implements RemoteFeedback {    

    private Feedback feedback;
    private int registryPort;
    private String serviceName;

    public RemoteConsoleFeedback(Configuration conf) throws IOException {
        super (getServicePort(conf));
        feedback = new ConsoleFeedback(conf);

         registryPort = conf.getInt (REGISTRY_PORT_PROPERTY, 27000);
         serviceName = conf.getString(SERVICE_NAME_PROPERTY,
                                            "remoteConsole");

        RemoteHelper.exportRemoteInterface(this, registryPort, serviceName);
    }

    public RemoteConsoleFeedback () throws IOException {
        this (Configuration.getSystemConfiguration(true));
    }

    private static int getServicePort (Configuration conf) {
        return conf.getInt(SERVICE_PORT_PROPERTY, 27091);
    }

    public void putMessages(List<Message> messages) throws RemoteException {
        try {
            feedback.putMessages(messages);
        } catch (IOException e) {
            throw new RemoteException("Broken output stream", e);
        }
    }

    public void putMessage(Message message) throws RemoteException {
        try {
            feedback.putMessage(message);
        } catch (IOException e) {
            throw new RemoteException("Broken output stream", e);
        }
    }

    public void close () throws IOException {
        RemoteHelper.unExportRemoteInterface(serviceName, registryPort);
    }
}
