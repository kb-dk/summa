package dk.statsbiblioteket.summa.control.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.feedback.FeedbackShellContext;
import dk.statsbiblioteket.summa.control.api.feedback.Feedback;

import java.util.List;
import java.util.Arrays;

/**
 * A {@link Runnable} helper class to monitor a {@link Monitorable} until
 * its status meets some condition.
 */
public class StatusMonitor implements Runnable {

    private static final Log log = LogFactory.getLog(StatusMonitor.class);

    private ConnectionManager<? extends Monitorable> connMgr;
    private Monitorable _mon;
    private String connectionId;
    private int timeout;
    private ShellContext ctx;
    private List<Status.CODE> ignoreStatuses;
    private ConnectionContext<? extends Monitorable> connCtx;


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
        this.ctx = ctx;
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
        this.ctx = ctx;
        this.ignoreStatuses = Arrays.asList(ignoreStatuses);

        log.trace ("Created StatusMonitor with statis connection and timeout "
                   + timeout  + ", ignoring statuses "
                   + Logs.expand(this.ignoreStatuses, 5) + ". "
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
        ctx.debug ("Waiting for status from " + connectionId + "...");
        log.trace ("Waiting for status from " + connectionId + "...");
        for (int tick = 0; tick < timeout; tick++) {

            try {
                Thread.sleep (1000);
            } catch (InterruptedException e) {
                // We should probably die if somebody interrupts us
                ctx.debug ("Status monitor interrupted");
                return;
            }

            try {
                Monitorable mon = getConnection();

                if (mon == null) {
                    ctx.info ("Connection to '" + connectionId + "', still not " +
                              "up. Waiting...");
                    continue;
                }

                Status s = mon.getStatus();

                if (ignoreStatuses.contains(s.getCode())) {
                    // Wait another interation
                    ctx.debug ("Got status: " + s + ". Waiting for update...");
                    log.trace ("Got status: " + s + ". Waiting for update...");
                    continue;
                }


                // If we reach this point we are good,
                // and the monitor should die
                if (connectionId != null) {
                    ctx.info ("'" + connectionId + "' reports status: " + s);
                    log.debug ("'" + connectionId + "' reports status: " + s);
                } else {
                    ctx.info ("Connection reports status: " + s);
                    log.debug ("Connection reports status: " + s);
                }

                return;
            } catch (Exception e) {
                ctx.error ("Failed to ping '" + connectionId
                           + "'. Error was:\n " + Strings.getStackTrace(e));
                log.warn ("Failed to ping '" + connectionId + "'", e);
            } finally {
                releaseConnection();
            }
        }
        ctx.error ("'" + connectionId + "' did not respond after "
                   + timeout + "s. It has probably crashed.");
        log.warn ("'" + connectionId + "' did not respond after "
                   + timeout + "s. It has probably crashed.");
    }

    private synchronized Monitorable getConnection() {
        if (_mon != null) {
            log.trace ("Returning static Monitorable connection");
            return _mon;
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
