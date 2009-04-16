package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.control.api.Monitorable;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.util.Strings;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link WorkflowStep} that blocks until the {@link Monitorable#getStatus}
 * of a {@link dk.statsbiblioteket.summa.control.api.Service} or
 * {@link dk.statsbiblioteket.summa.control.api.ClientConnection} returns
 * a status code in some defined set of allowed states.
 * <p/>
 * By default the {@link #run} method throws an exception if the state of the
 * monitored object becomes {@link Status.CODE#crashed}, but this is
 * configurable via the  {@link #CONF_BAD_STATES} property.
 * <p/>
 * The connection to the monitorable is done via a {@link ConnectionConsumer},
 * meaning that the connection is configured by setting the
 * {@link ConnectionConsumer#CONF_RPC_TARGET} in the {@code WaitForStatusStep}s
 * configuration.
 */
public class WaitForStatusStep extends ConnectionConsumer<Monitorable>
                               implements WorkflowStep {

    /**
     * Property defining a list of strings, of enumeration names of the
     * {@link Status.CODE} enumeration. If the monitored object reports
     * any of the status codes listed in this in this property the workflow
     * step will complete. Default value is {@link #DEFAULT_GOOD_STATES}
     */
    public static final String CONF_GOOD_STATES =
                                 "summa.workflow.step.waitforstatus.goodstates";

    /**
     * Default value for {@link #CONF_GOOD_STATES}. A list containing the
     * {@code idle} {@link Status.CODE}.
     */
    public static final List<String> DEFAULT_GOOD_STATES =
                                     Arrays.asList(Status.CODE.idle.toString());

    /**
     * List of {@link Status.CODE} value names. If the monitored object reports
     * any of the states listed here the workflow step will throw a
     * {@link BadStateException}. Default value is {@link #DEFAULT_BAD_STATES}
     */
    public static final String CONF_BAD_STATES =
                                  "summa.workflow.step.waitforstatus.badstates";

    /**
     * Default value for the {@link #CONF_BAD_STATES} property. A list of
     * strings containing only the {@link Status.CODE} {@code idle}.
     */
    public static final List<String> DEFAULT_BAD_STATES =
                                  Arrays.asList(Status.CODE.crashed.toString());

    /**
     * Property defining whether or not to allow the
     * {@link Monitorable#getStatus} to throw an {@link IOException}. If this
     * property is {@code true} and {@code getStatus()} throws an
     * {@code IOException} the {@code getStatus()} requested will be re-issued
     * after a small timeout.
     * <p/>
     * If this property is {@code false} the {@code IOException} will be
     * propagated to the caller wrapped in a {@link RuntimeException}.
     * <p/>
     * Default value {@link #DEFAULT_FAILURE_TOLERANT}
     */
    public static final String CONF_FAILURE_TOLERANT =
                            "summa.workflow.step.waitforstatus.failuretolerant";

    /**
     * Default value for the {@link #CONF_FAILURE_TOLERANT} property
     */
    public static final boolean DEFAULT_FAILURE_TOLERANT = true;

    /**
     * Exception throw when encountering a state defined in
     * {@link WaitForStatusStep#CONF_BAD_STATES}
     */
    public static class BadStateException extends RuntimeException {

        private Status stat;

        public BadStateException (String msg, Status stat) {
            super(msg);

            this.stat = stat;
        }

        public Status getStatus() {
            return stat;
        }
    }

    private List<Status.CODE> goodStates;
    private List<Status.CODE> badStates;
    private boolean failureTolerant;
    private Log log;        

    public WaitForStatusStep (Configuration conf) {
        super(conf);

        log = LogFactory.getLog(this.getClass().getName());

        failureTolerant = conf.getBoolean(CONF_FAILURE_TOLERANT,
                                          DEFAULT_FAILURE_TOLERANT);

        List<String> _goodStates = conf.getStrings(CONF_GOOD_STATES,
                                                   DEFAULT_GOOD_STATES);
        List<String> _badStates = conf.getStrings(CONF_BAD_STATES,
                                                   DEFAULT_BAD_STATES);

        goodStates = new ArrayList<Status.CODE>(_goodStates.size());
        badStates = new ArrayList<Status.CODE>(_badStates.size());

        for (String code : _goodStates) {
            try {
                goodStates.add(Status.CODE.valueOf(code));
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("In " + CONF_GOOD_STATES
                                                 + ", bad status code '"
                                                 + code + "'");
            }
        }

        for (String code : _badStates) {
            try {
                badStates.add(Status.CODE.valueOf(code));
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("In " + CONF_BAD_STATES
                                                 + ", bad status code '"
                                                 + code + "'");
            }
        }

        log.debug("Failure tolerant: " + failureTolerant);
        log.debug("Configured to wait for: " + Strings.join(goodStates, ", "));
        log.debug("Will raise errors on: " + Strings.join(badStates, ", "));
    }

    public void run() {
        while(true) {
            Monitorable mon = getConnection();
            Status stat;
            try {
                stat = mon.getStatus();
            } catch (IOException e) {
                if (!failureTolerant) {
                    throw new RuntimeException("Error connecting to monitorable "
                                               + getVendorId() + ": "
                                               + e.getMessage(), e);
                }

                // Log on debug, but only log stack trace if we are tracing
                if (log.isTraceEnabled()) {
                    log.debug("Error connecting to monitorable "
                              + getVendorId() + ": "
                              + e.getMessage(), e);
                } else {
                    log.debug("Error connecting to monitorable "
                              + getVendorId() + ": " + e.getMessage());
                }

                try {
                    // TODO: Make the sleep time configurable?
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    log.warn("Interrupted while recovering from error. "
                             + "Aborting workflow step");
                    break;
                }

                continue;

            } finally {
                releaseConnection();
            }

            if (goodStates.contains(stat.getCode())) {
                log.debug("Got good state " + stat.getCode());
                return;
            } else if (badStates.contains(stat.getCode())) {
                log.debug("Got bad state " + stat.getCode());
                throw new BadStateException("Got bad state " + stat
                                            + " from " + getVendorId(), stat);
            }
        }
    }
}

