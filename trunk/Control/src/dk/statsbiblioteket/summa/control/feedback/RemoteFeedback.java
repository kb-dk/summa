package dk.statsbiblioteket.summa.control.feedback;

import dk.statsbiblioteket.summa.control.api.Feedback;
import dk.statsbiblioteket.summa.control.api.Message;

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
    public static final String SERVICE_PORT_PROPERTY = "summa.control.remoteConsole.service.port";

    /**
     * Configuration property specifying the name of the service exposing the
     * {@link Feedback} interface. Default is "remoteConsole".
     */
    public static final String SERVICE_NAME_PROPERTY = "summa.control.remoteConsole.service.name";

    /**
     * Configuration property specifying the port on which the registry should
     * run. Default is 27000.
     */
    public static final String REGISTRY_PORT_PROPERTY = "summa.control.remoteConsole.registry.port";

    /**
     * Configuration property specifying the host on which the registry should
     * run. Default is "localhost".
     */
    public static final String REGISTRY_HOST_PROPERTY = "summa.control.remoteConsole.registry.host";

    public void putMessages(List<Message> messages) throws RemoteException;

    public void putMessage(Message message) throws RemoteException;

}
