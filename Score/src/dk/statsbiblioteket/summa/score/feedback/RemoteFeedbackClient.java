package dk.statsbiblioteket.summa.score.feedback;

import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.summa.score.api.Message;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import dk.statsbiblioteket.util.Logs;

import java.util.List;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Feedback} implementation talking to a {@link RemoteFeedback}.
 * This is for example used to implement remote consoles, where an instance of
 * this class is running on the Score server, and a
 * {@link RemoteConsoleFeedback} is running in a
 * {@link dk.statsbiblioteket.summa.score.server.shell.ScoreShell}.
 */
public class RemoteFeedbackClient implements Feedback {

    private ConnectionManager<Feedback> cm;
    private String remoteAddress;
    private Log log = LogFactory.getLog (RemoteFeedbackClient.class);

    public RemoteFeedbackClient(Configuration conf) {
        cm = new ConnectionManager<Feedback> (new RMIConnectionFactory<RemoteFeedback>());


        remoteAddress = "//" 
                       + conf.getString(RemoteFeedback.REGISTRY_HOST_PROPERTY,
                                        "localhost")
                       + ":"
                       + conf.getInt(RemoteFeedback.REGISTRY_PORT_PROPERTY,
                                     27000)
                       + "/"
                       + conf.getString(RemoteFeedback.SERVICE_NAME_PROPERTY,
                                        "remoteConsole");

        log.debug ("Using remote console on '" + remoteAddress + "'");

    }

    public RemoteFeedbackClient () {
        this (Configuration.getSystemConfiguration(true));
    }

    public void putMessages(List<Message> messages) {
        ConnectionContext<Feedback> conn = null;
        try {
            conn = cm.get (remoteAddress);
            if (conn == null) {
                log.error ("Error putting messages " + Logs.expand(messages, 10)
                          + ". Failed to connect to " + remoteAddress);
                return;
            }
            Feedback feedback = conn.getConnection();
            try {
                feedback.putMessages(messages);
            } catch (IOException e) {
                log.error ("Error putting messages " + Logs.expand(messages, 10)
                          + ". Error was ", e);
            }
        } finally {
            if (conn != null) {
                cm.release (conn);
            }
        }
    }

    public void putMessage(Message message) throws IOException {
        ConnectionContext<Feedback> conn = null;
        try {
            conn = cm.get (remoteAddress);
            if (conn == null) {
                log.error ("Error putting message '" + message + "'. Failed to "
                           + "connect to " + remoteAddress);
                return;
            }
            Feedback feedback = conn.getConnection();
            try {
                feedback.putMessage(message);
            } catch (IOException e) {
                log.error("Error putting message '" + message + "' . Error was ",
                          e);
            }
        } finally {
            if (conn != null) {
                cm.release (conn);
            }
        }
    }
}
