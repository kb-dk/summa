package dk.statsbiblioteket.summa.control.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.VoidShellContext;
import dk.statsbiblioteket.summa.control.api.feedback.FeedbackShellContext;
import dk.statsbiblioteket.summa.control.api.feedback.Feedback;

import java.util.List;
import java.util.Arrays;
import java.io.IOException;

/**
 * A {@link Runnable} helper class to monitor a {@link Monitorable} until
 * its status meets some condition.
 */
public class StatusMonitor implements Runnable {

    private static final Log log = LogFactory.getLog(StatusMonitor.class);

    private ConnectionManager<? extends Monitorable> connMgr;
    private ConnectionFactory<? extends Monitorable> connFact;
    private Monitorable _mon;
    private String connectionId;
    private int timeout;
    private int tick;
    private ShellContext ctx;
    private List<Status.CODE> ignoreStatuses;
    private ConnectionContext<? extends Monitorable> connCtx;
    private Status lastStatus;


    /**
     * <p>Create a new status monitor that will wait until the connection
     * answers to a {@link Monitorable#getStatus()} request.</p>
     *
     * <p>You may pass in a series of {@link Status.CODE}s that should not
     * trigger monitor termination. For example: If you want to wait until the
     * monitorable is fully instantiated then pass the state
     * {@link Status.CODE#not_instantiated} as the {@code ignoreStatuses} argument.
     * This way the monitor will not return before the object reports something
     * other than {@code Status.CODE.not_instantiated}</p>
     *
     * <p>The monitor will print an error to the {@link ShellContext} if the
     * specified {@link Monitorable} does not respond within {@code timeout}
     * seconds.</p>
     *
     * @param connMgr Connection manager used to obtain connections to the object
     *                to monitor
     * @param connectionId id passed to {@code connMgr} to obtain a connection
     *                     to the monitorable
     * @param timeout Number of seconds before the connection times out
     * @param ctx ShellContext to print to in case of errors
     * @param ignoreStatuses a collection of states that should not trigger monitor
     *              termination
     */
    public StatusMonitor (ConnectionManager<? extends Monitorable> connMgr,
                          String connectionId,
                          int timeout,
                          ShellContext ctx,
                          Status.CODE... ignoreStatuses) {
        this.connMgr = connMgr;
        this.connectionId = connectionId;
        this.timeout = timeout;
        this.ctx = ctx != null ? ctx : new VoidShellContext();
        this.ignoreStatuses = Arrays.asList(ignoreStatuses);

        log.trace ("Created StatusMonitor with timeout " + timeout  + ", ignoring"
                   + " statuses " + Logs.expand(this.ignoreStatuses, 5) + ". "
                   + "Outputting to a " + ctx);
    }

    public StatusMonitor (Monitorable mon,
                          int timeout,
                          ShellContext ctx,
                          Status.CODE... ignoreStatuses) {
        this._mon = mon;
        this.timeout = timeout;
        this.ctx = ctx != null ? ctx : new VoidShellContext();
        this.ignoreStatuses = Arrays.asList(ignoreStatuses);

        log.trace ("Created StatusMonitor with static connection and timeout "
                   + timeout  + ", ignoring statuses "
                   + Logs.expand(this.ignoreStatuses, 5) + ". "
                   + "Outputting to a " + ctx);
    }

    public StatusMonitor (ConnectionFactory<? extends Monitorable> connFact,
                          String connectionId,
                          int timeout,
                          ShellContext ctx,
                          Status.CODE... ignoreStatuses) {
        this.connFact = connFact;
        this.connectionId = connectionId;
        this.timeout = timeout;
        this.ctx = ctx != null ? ctx : new VoidShellContext();
        this.ignoreStatuses = Arrays.asList(ignoreStatuses);

        // Configure the connection factory so we can uphold our timig promises
        connFact.setGraceTime(1);
        connFact.setNumRetries(1);

        log.trace ("Created StatusMonitor with timeout " + timeout  + ", ignoring"
                   + " statuses " + Logs.expand(this.ignoreStatuses, 5) + ". "
                   + "Outputting to a " + ctx);
    }

    /**
     * Same as other constructor, except that any user feedback is delivered
     * via a {@link Feedback} instead of a {@link ShellContext}.
     */
    public StatusMonitor (ConnectionManager<? extends Monitorable> connMgr,
                          String connectionId,
                          int timeout,
                          Feedback feedback,
                          Status.CODE... ignoreStatuses) {
        this.connMgr = connMgr;
        this.connectionId = connectionId;
        this.timeout = timeout;
        this.ctx = new FeedbackShellContext(feedback);
        this.ignoreStatuses = Arrays.asList(ignoreStatuses);

        log.trace ("Created StatusMonitor for " + connectionId
                   + "with timeout " + timeout  + ", ignoring"
                   + " statuses " + Logs.expand(this.ignoreStatuses, 5) + ". "
                   + "Outputting to a " + feedback);
    }

    public void run() {
        String msg = "Waiting for status"
                   + (connectionId == null ? "" : ("from " + connectionId))
                   + "...";
        log.debug(msg);

        try {
            updateStatus();
        } catch (IOException e) {
            lastStatus = new Status(Status.CODE.crashed, msg);
        }

        for (tick = 0; tick < timeout; tick++) {

            try {
                Thread.sleep (1000);
            } catch (InterruptedException e) {
                // We should probably die if somebody interrupts us
                ctx.warn("Status monitor interrupted");
                return;
            }

            try {
                updateStatus();

                if (ignoreStatuses.contains(lastStatus.getCode())) {
                    // Wait another interation
                    msg = "Got status: "
                          + lastStatus + ". Waiting for update...";
                    log.trace (msg);
                    continue;
                }


                // If we reach this point we are good,
                // and the monitor should die
                if (connectionId != null) {
                    msg = "'" + connectionId + "'"
                          +" reports status: " + lastStatus;
                } else {
                    msg = "Connection reports status: " + lastStatus;
                }
                log.debug (msg);

                return;

            } catch (Exception e) {
                if (connectionId == null) {
                    msg = "Ping failed, error was:\n"+ Strings.getStackTrace(e);
                } else {
                    msg = "Failed to ping '" + connectionId
                           + "', error was:\n " + Strings.getStackTrace(e);
                }
                ctx.error (msg);
                log.warn ("Failed to ping '" + connectionId + "'", e);

                lastStatus = new Status(Status.CODE.crashed,
                                        msg);
            } finally {
                releaseConnection();
            }
        }

        if (lastStatus == null) {
            if (connectionId == null) {
                msg = "No response after " + timeout + "s. "
                      + "The endpoint has probably crashed";
            } else {
                msg = "'" + connectionId + "' did not respond after "
                      + timeout + "s. It has probably crashed.";
            }

            ctx.error (msg);
            log.warn (msg);
            lastStatus = new Status(Status.CODE.crashed, msg);

        } else {
            log.warn("No valid state within " + timeout + "s. "
                     + "Last status was: " + lastStatus);
        }

    }

    private void reportProgress() {
        ctx.prompt(".");
    }

    /**
     * Retrieve the last {@link Status} that was retrieved from the monitored
     * object.
     * @return the last status that was retrieved from the monitored object.
     *         In case no status has been retrieved yet the status will be
     *         {@code null}
     */
    public Status getLastStatus() {
        return lastStatus;
    }

    /**
     * Check if the monitor has timed out. The monitor is timed out if the
     * {@code run()} method has completed and the last checked status was
     * one of the invalid states.
     * @return {@code true} if the {@code run()} method has completed and
     *         the last status checked was in the list of ignored states
     */
    public boolean isTimedOut() {
        return (tick >= timeout)
               && ignoreStatuses.contains(lastStatus.getCode());
    }

    private void updateStatus() throws IOException {
        reportProgress();
        Monitorable mon = getConnection();

        if (mon == null) {
            String msg = connectionId == null ?
                         "Connection still not up. Waiting..." :
                         ("Connection to '" + connectionId
                          + "', still not up. Waiting...");
            lastStatus = new Status(Status.CODE.not_instantiated, msg);
            return;
        }

        try {
            lastStatus = mon.getStatus();
        } catch (InvalidServiceStateException e) {
            lastStatus = new Status(Status.CODE.not_instantiated,
                                    "Connection up" +
                                    (connectionId == null ?
                                           "": (" to " + connectionId))
                                    + ", but service is not ready"); 
        }
    }

    private synchronized Monitorable getConnection() {
        if (_mon != null) {
            log.trace ("Returning static Monitorable connection");
            return _mon;
        }

        if (connFact != null) {
            log.debug("Creating connection to " + connectionId);
            return connFact.createConnection(connectionId);
        }

        if (connCtx == null) {
            connCtx = connMgr.get(connectionId);
        }

        if (connCtx == null) {
            return null;
        }

        return connCtx.getConnection();
    }

    private synchronized void releaseConnection() {
        if (_mon != null) {
            log.trace ("Ignoring release requeston static "
                       + "Monitorable connection");
            return;
        }

        if (connCtx == null) {
            log.debug ("Status monitor is trying to release its connection, "
                      + "but has never aquired one. This is an internal bug"
                      + " in the Summa Control");
            return;
        }

        connCtx.unref();
        connCtx = null;
    }

}



