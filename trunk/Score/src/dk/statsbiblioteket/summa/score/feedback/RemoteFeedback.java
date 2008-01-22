package dk.statsbiblioteket.summa.score.feedback;

import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.summa.score.api.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 *
 */
public interface RemoteFeedback extends Remote, Feedback {

    /**
     * Configuration property naming the port on which the RMI service should
     * communicate. Default is 27091.
     */
    public static final String SERVICE_PORT_PROPERTY = "summa.score.remoteConsole.service.port";

    /**
     * Configuration property specifying the name of the service exposing the
     * {@link Feedback} interface. Default is "remoteConsole".
     */
    public static final String SERVICE_NAME_PROPERTY = "summa.score.remoteConsole.service.name";

    /**
     * Configuration property specifying the port on which the registry should
     * run. Default is 27000.
     */
    public static final String REGISTRY_PORT_PROPERTY = "summa.score.remoteConsole.registry.port";

    /**
     * Configuration property specifying the host on which the registry should
     * run. Default is "localhost".
     */
    public static final String REGISTRY_HOST_PROPERTY = "summa.score.remoteConsole.registry.host";

    public void putMessages(List<Message> messages) throws RemoteException;

    public void putMessage(Message message) throws RemoteException;

}
