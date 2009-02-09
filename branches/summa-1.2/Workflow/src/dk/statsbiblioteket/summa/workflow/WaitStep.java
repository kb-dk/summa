package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link WorkflowStep} that simply blocks for a configuable amount of time
 * before returning.
 */
public class WaitStep implements WorkflowStep {

    /**
     * Number of seconds to block. Default is {@link #DEFAULT_TIME}
     */
    public static final String CONF_TIME = "summa.workflow.step.wait.time";

    /**
     * Default number of seconds to block
     */
    public static final int DEFAULT_TIME = 60;

    private Log log;
    private int blockTime;

    /**
     * Create a new {@code WaitStep} blocking {@code blockTime} seconds
     * @param blockTime number of seconds to block
     */
    public WaitStep (int blockTime) {
        log = LogFactory.getLog(this.getClass().getName());

        log.debug("Block time " + blockTime + "s");

        this.blockTime = blockTime;
    }

    public WaitStep (Configuration conf) {
        this(conf.getInt(CONF_TIME, DEFAULT_TIME));
    }

    public void run() {
        log.debug("Blocking for " + blockTime + "s");

        try {
            Thread.sleep(blockTime*1000);
        } catch (InterruptedException e) {
            log.warn("Interrupted while blocking");
        }
    }
}
