package dk.statsbiblioteket.summa.control.server;

import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.Feedback;
import dk.statsbiblioteket.summa.control.feedback.FeedbackShellContext;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link Runnable} helper class to monitor a {@link ClientConnection} until
 * its status meets some condition.
 */
public class ClientMonitor implements Runnable {

    private static final Log log = LogFactory.getLog(ClientMonitor.class);

    private ConnectionManager<ClientConnection> connMgr;
    private String clientId;
    private int timeout;
    private ShellContext ctx;
    private List<Status.CODE> ignoreStatuses;
    private ConnectionContext<ClientConnection> connCtx;


    /**
     * <p>Create a new client monitor that will wait until the client
     * answers to a {@link ClientConnection#getStatus()} request.</p>
     *
     * <p>You may pass in a series of {@link Status.CODE}s that should not
     * trigger monitor termination. For example: If you want to wait until the
     * client is fully instantiated then pass the state
     * {@link Status.CODE#not_instantiated} as the {@code ignoreStatuses} argument.
     * This way the monitor will not return before the client reports something
     * other than {@code Status.CODE.not_instantiated}</p>
     *
     * <p>The monitor will print an error to the {@link ShellContext} if the
     * specified {@link ClientConnection} does not respond within {@code timeout}
     * seconds.</p>
     *
     * @param connMgr The client controlling service to monitor
     * @param clientId id of the service to monitor
     * @param timeout Number of seconds before the connection times out
     * @param ctx ShellContext to print to in case of errors
     * @param ignoreStatuses a collection of states that should not trigger monitor
     *              termination
     */
    public ClientMonitor (ConnectionManager<ClientConnection> connMgr,
                          String clientId,
                          int timeout,
                          ShellContext ctx,
                          Status.CODE... ignoreStatuses) {
        this.connMgr = connMgr;
        this.clientId = clientId;
        this.timeout = timeout;
        this.ctx = ctx;
        this.ignoreStatuses = Arrays.asList(ignoreStatuses);

        log.trace ("Created ClientMonitor with timeout " + timeout  + ", ignoring"
                   + " statuses " + Logs.expand(this.ignoreStatuses, 5) + ". "
                   + "Outputting to a " + ctx);
    }

    /**
     * Same as other constructor, except that any user feedback is delivered
     * via a {@link Feedback} instead of a {@link ShellContext}.
     */
    public ClientMonitor (ConnectionManager<ClientConnection> connMgr,
                          String clientId,
                          int timeout,
                          Feedback feedback,
                          Status.CODE... ignoreStatuses) {
        this.connMgr = connMgr;
        this.clientId = clientId;
        this.timeout = timeout;
        this.ctx = new FeedbackShellContext(feedback);
        this.ignoreStatuses = Arrays.asList(ignoreStatuses);

        log.trace ("Created ClientMonitor for " + clientId
                   + "with timeout " + timeout  + ", ignoring"
                   + " statuses " + Logs.expand(this.ignoreStatuses, 5) + ". "
                   + "Outputting to a " + feedback);
    }

    public void run() {
        ctx.debug ("Waiting for client " + clientId + "...");
        log.trace ("Waiting for client " + clientId + "...");
        for (int tick = 0; tick < timeout; tick++) {

            try {
                Thread.sleep (1000);
            } catch (InterruptedException e) {
                // We should probably die if somebody interrupts us
                ctx.debug ("Client monitor interrupted");
                return;
            }

            try {
                ClientConnection client = getClientConnection();
                Status s = client.getStatus();

                if (ignoreStatuses.contains(s.getCode())) {
                    // Wait another interation
                    ctx.debug ("Client returned status: " + s
                               + ". Waiting for update...");
                    log.trace ("Client returned status: " + s
                               + ". Waiting for update...");
                    continue;
                }


                // If we reach this point we are good,
                // and the monitor should die
                ctx.debug ("Connection to client '" + clientId
                           + "' up. Status: " + s);
                log.debug ("Connection to client '" + clientId
                           + "' up. Status: " + s);
                return;
            } catch (Exception e) {
                ctx.error ("Failed to ping client '" + clientId
                           + "'. Error was:\n " + Strings.getStackTrace(e));
                log.warn ("Failed to ping client '" + clientId + "'", e);
            } finally {
                releaseClientConnection();
            }
        }
        ctx.error ("Client '" + clientId + "' did not respond after "
                   + timeout + "s. It has probably crashed.");
        log.warn ("Client '" + clientId + "' did not respond after "
                   + timeout + "s. It has probably crashed.");
    }

    private synchronized ClientConnection getClientConnection () {

        if (connCtx == null) {
            connCtx = connMgr.get(clientId);
        }

        return connCtx.getConnection();
    }

    private synchronized void releaseClientConnection () {
        if (connCtx == null) {
            log.debug ("Client monitor is trying to release its connection, "
                      + "but has never aquired one. This is an internal bug"
                      + " in the Summa Control");
            return;
        }

        connCtx.unref();
        connCtx = null;
    }

}
