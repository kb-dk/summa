package dk.statsbiblioteket.summa.control.api.feedback.rmi;

import dk.statsbiblioteket.summa.control.api.feedback.Feedback;
import dk.statsbiblioteket.summa.control.api.feedback.Message;

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
    public static final String CONF_SERVICE_PORT = "summa.control.remoteconsole.service.port";

    /**
     * Configuration property specifying the name of the service exposing the
     * {@link Feedback} interface. Default is "remoteConsole".
     */
    public static final String CONF_SERVICE_NAME = "summa.control.remoteconsole.service.name";

    /**
     * Configuration property specifying the port on which the registry should
     * run. Default is 27000.
     */
    public static final String CONF_REGISTRY_PORT = "summa.control.remoteconsole.registry.port";

    /**
     * Configuration property specifying the host on which the registry should
     * run. Default is "localhost".
     */
    public static final String CONF_REGISTRY_HOST = "summa.control.remoteconsole.registry.host";

    public void putMessages(List<Message> messages) throws RemoteException;

    public void putMessage(Message message) throws RemoteException;

}



