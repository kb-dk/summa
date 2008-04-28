package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * <p>Thrown when somebody tries to perform an action on a service
 * which does not make sense in the service's current state.</p>
 *
 * <p>For example thrown if somebody tries to stop a non-running service.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class InvalidServiceStateException extends ClientException {
    /**
     *
     * @param client the client encountering the exceptional state
     * @param serviceId the id of the service which is involved
     * @param action the action which was requested to be performed on the service
     * @param cause Short string describing the cause of the problem
     */
    public InvalidServiceStateException (Client client, String serviceId,
                                         String action, String cause) {
        super (client, "Request '" + action + "' on service '" + serviceId
                + "' is illegal. Cause: " + cause);
    }
}
