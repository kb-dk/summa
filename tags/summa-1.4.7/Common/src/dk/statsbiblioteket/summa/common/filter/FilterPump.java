/**
 * Created: te 18-02-2008 23:59:10
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.filter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.StateThread;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sets up a chain of ObjectFilters and pumps the last filter until no more data
 * can be retrieved.
 * </p><p>
 * The setup of the filters is done with {@link FilterSequence}. See the JavaDoc
 * for that class for property requirements.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterPump extends StateThread implements Configurable {
    /* We delay the creation of the log until we know the name of the chain. */
    private Log log;
    private static Log classLog = LogFactory.getLog(FilterPump.class);

    /**
     * The name of the chain if no name is stated by
     * {@link Filter#CONF_FILTER_NAME}.
     */
    public static final String DEFAULT_CHAIN_NAME = "Unnamed Chain";
    private FilterSequence sequence;

    private String chainName = DEFAULT_CHAIN_NAME;
    private static final int DEBUG_FEEDBACK = 100;
    private static final int INFO_FEEDBACK = 10000;


    long objectCounter = 0;
    long streamBytesCounter = 0;

    public FilterPump(Configuration configuration) throws IOException {
        classLog.trace ("Constructing FilterPump with config class "
                        + configuration.getClass());
        chainName = configuration.getString(Filter.CONF_FILTER_NAME, chainName);
        classLog.trace ("Creating chain log for chain: " + chainName);
        log = LogFactory.getLog(FilterPump.class.getName() + "#" + chainName);
        log.info("Constructing FilterPump for chain '" + chainName + "'");
        sequence = new FilterSequence(configuration);
        log.debug("Constructed filter sequence");
    }

    // TODO: Better feedback with Profiler

    /**
     * The runMethod is normally managed by the {@link #start} and
     * {@link #stop} methods of FilterPump. It is not advisable to call it
     * explicitly.
     */
    @Override
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    protected void runMethod() {
        log.debug("Running FilterChain '" + chainName + "'");
        Profiler profiler = new Profiler();
        profiler.setBpsSpan(1000);
        try {
            long startTime;
            while (getStatus() == STATUS.running) {
                startTime = System.nanoTime();
                if (!sequence.pump()) {
                    profiler.beat();
                    log.info(String.format(
                            "Finished pumping '%s' %d times in %s, overall "
                            + "average speed was %s pumps/sec",
                            chainName, profiler.getBeats(),
                            profiler.getSpendTime(), profiler.getBps(false)));
                    break;
                }
                profiler.beat();
                logStatistics(profiler, startTime);
            }
        } catch (IOException e) {
            String error = "IOException caught running FilterPump";
            log.error(error, e);
            setError(error, e);
        } catch (Throwable t) {
            String error = "Throwable caught running FilterPump";
            log.error(error, t);
            setError(error, t);
        }
        log.debug("Finished run with status " + getStatus());
        if (STATUS.error.equals(getStatus())) {
            log.warn("The run was finished with error '" + getErrorMessage(),
                     getErrorCause());
        } else {
            log.debug("No error in run, calling close(true)");
        }
        // TODO: Check if this is redundant - doesn't EOF handle it? 
        close(true);
    }

    private void logStatistics(Profiler profiler, long startTime) {
        if (!(log.isTraceEnabled()
              || (log.isDebugEnabled()
                  && profiler.getBeats() % DEBUG_FEEDBACK == 0)
              || (log.isInfoEnabled()
                  && profiler.getBeats() % INFO_FEEDBACK == 0))) {
            return;
        }
        String ms =
                Double.toString((System.nanoTime() - startTime) / 1000000.0);
        String message = String.format(
                "%d pumps performed in %s, average speed for the last %d pumps"
                + " was %s pumps/sec, overall average was %s pumps/sec, last"
                + " pump took %s ms",
                profiler.getBeats(), profiler.getSpendTime(),
                profiler.getBpsSpan(), profiler.getBps(true),
                profiler.getBps(false), ms);
        if (profiler.getBeats() % INFO_FEEDBACK == 0) {
            log.info(message);
        } else if (profiler.getBeats() % DEBUG_FEEDBACK == 0) {
            log.debug(message); 
        } else {
            log.trace(message);
        }
    }

    @Override
    public void stop() {
        log.info("Stopping filter pump " + getChainName());
        super.stop();

        sequence.close(true);
        log.info(String.format("Filter pump %s stopped", getChainName()));
    }

    private void close(boolean success) {
        sequence.close(success);
    }

    public String getChainName() {
        return chainName;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter(500);
        sw.append(getStatus().toString()).append(": ");
        sw.append(sequence.toString());
        sw.append(" pump");
        return sw.toString();
    }

    /**
     * @return a shallow copy of the list of the filters in this pump.
     */
    public List<Filter> getFilters() {
        return sequence.getFilters();
    }
}
