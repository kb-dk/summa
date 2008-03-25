/**
 * Created: te 18-02-2008 23:59:10
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.StateThread;
import dk.statsbiblioteket.summa.ingest.records.RecordFilter;
import dk.statsbiblioteket.summa.ingest.stream.StreamFilter;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sets up a chain of filters and pumps the last filter until EOF is reached.
 * The filters are specified with {@link #CONF_STREAM_FILTERS} and
 * {@link #CONF_RECORD_FILTERS} which contains arrays of keys for the
 * configuration. For each key, a subConfiguration is retrieved. The
 * subConfiguration contains the class-name of the wanted filter in the
 * property {@link #CONF_FILTER_CLASS} along with the filter-specific setup.
 * </p><p>
 * RecordFilters are chained after SteamFilters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterPump extends StateThread implements Configurable {
    /* We delay the creation of the log until we know the name of the chain. */
    private Log log;

    public static final String CONF_STREAM_FILTERS = "FilterPump.StreamFilters";
    public static final String CONF_RECORD_FILTERS = "FilterPump.RecordFilters";
    /**
     * The name of the chain is used for feedback and debugging.
     */
    public static final String CONF_CHAIN_NAME =     "FilterPump.ChainName";
    public static final String CONF_FILTER_CLASS = "FilterPump.FilterClass";

    private ArrayList<StreamFilter> streamFilters =
            new ArrayList<StreamFilter>(10);
    private StreamFilter lastStreamFilter;
    private ArrayList<RecordFilter> recordFilters =
            new ArrayList<RecordFilter>(10);
    private RecordFilter lastRecordFilter;

    private String chainName = "Unnamed Chain";

    long recordCounter = 0;
    long streamBytesCounter = 0;

    public FilterPump(Configuration configuration) throws IOException {
        chainName = configuration.getString(CONF_CHAIN_NAME, chainName);
        log = LogFactory.getLog(FilterPump.class + "." + chainName);
        log.info("Constructing FilterPump for chain '" + chainName + "'");
        List<String> streamFilterNames = null;
        try {
            streamFilterNames = configuration.getStrings(CONF_STREAM_FILTERS);
        } catch (NullPointerException e) {
            log.info("No StreamFilters specified for FilterPump");
        }
        List<String> recordFilterNames = null;
        try {
            recordFilterNames = configuration.getStrings(CONF_RECORD_FILTERS);
            //noinspection DuplicateStringLiteralInspection
            Logs.log(log, Logs.Level.INFO, "Building record filter chain with ",
                     recordFilterNames);
        } catch (NullPointerException e) {
            log.info("No RecordFilters specified for FilterPump");
        }
        if (streamFilterNames == null && recordFilterNames == null) {
            throw new IllegalArgumentException("Neither stream- nor "
                                               + "record-filters are defined");
        }

        buildChain(configuration, streamFilterNames, recordFilterNames);
        log.debug("Finished building chain '" + chainName + "'");
    }

    // TODO: Too much redundancy. Consider a superclass for filters
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private void buildChain(Configuration configuration,
                            List<String> streamFilterNames,
                            List<String> recordFilterNames) throws IOException {
        log.trace("Entering buildChain for '" + chainName + "'");
        if (streamFilterNames != null) {
            Logs.log(log, Logs.Level.INFO, "Building stream filter chain with ",
                     streamFilterNames);
            for (String streamFilterName: streamFilterNames) {
                log.debug("Adding StreamFilter '" + streamFilterName
                          + "' to chain");
                Configuration streamConfiguration =
                        configuration.getSubConfiguration(streamFilterName);
                StreamFilter streamFilter =
                        createStreamFilter(streamConfiguration);
                if (lastStreamFilter != null) {
                    log.trace("Chaining '" + streamFilter + "' to the end of '"
                              + lastStreamFilter + "'");
                    streamFilter.setSource(lastStreamFilter);
                }
                lastStreamFilter = streamFilter;
                streamFilters.add(lastStreamFilter);
            }
        }
        if (recordFilterNames != null) {
            Logs.log(log, Logs.Level.INFO, "Building record filter chain with ",
                     recordFilterNames);
            for (String recordFilterName: recordFilterNames) {
                log.debug("Adding RecordFilter '" + recordFilterName
                          + "' to chain");
                Configuration recordConfiguration =
                        configuration.getSubConfiguration(recordFilterName);
                RecordFilter recordFilter =
                        createRecordFilter(recordConfiguration);
                if (lastRecordFilter != null) {
                    log.trace("Chaining '" + recordFilter + "' to the end of '"
                              + lastRecordFilter + "'");
                    recordFilter.setSource(lastRecordFilter);
                } else if (lastStreamFilter != null) {
                    log.trace("Chaining '" + recordFilter + "' to the end of '"
                              + lastStreamFilter + "'");
                    recordFilter.setSource(lastStreamFilter);
                }
                lastRecordFilter = recordFilter;
                recordFilters.add(lastRecordFilter);
            }
        }
        log.trace("Exiting buildChain for '" + chainName + "'");
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private StreamFilter createStreamFilter(Configuration configuration) {
        Class<? extends StreamFilter> streamFilter =
                configuration.getClass(CONF_FILTER_CLASS, StreamFilter.class);
        log.debug("Got stream filter class " + streamFilter
                  + ". Commencing creation");
        return configuration.create(streamFilter);
    }
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private RecordFilter createRecordFilter(Configuration configuration) {
        Class<? extends RecordFilter> recordFilter =
                configuration.getClass(CONF_FILTER_CLASS, RecordFilter.class);
        log.debug("Got record filter class " + recordFilter
                  + ". Commencing creation");
        return configuration.create(recordFilter);
    }

    // TODO: Better feedback with Profiler

    /**
     * The run method is normally managed by the {@link #start} and
     * {@link #stop} methods of FilterPump. It is not advisable to call it
     * explicitly.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void run() {
        log.debug("Running FilterChain '" + chainName + "'");
        try {
            while (getStatus() == STATUS.running) {
                if (lastRecordFilter != null) {
                    if (lastRecordFilter.getNextRecord() == null) {
                        log.info("Finished pumping " + recordCounter
                                 + " records from '" + chainName + "'");
                        break;
                    }
                    recordCounter++;
                } else {
                    if (lastStreamFilter.read() == StreamFilter.EOF) {
                        log.info("Finished pumping " + streamBytesCounter
                                 + " bytes from '" + chainName + "'");
                        break;
                    }
                    streamBytesCounter++;
                }
            }
        } catch (IOException e) {
            log.error("IOException caught running FilterPump", e);
            setError();
        } catch (Throwable t) {
            log.error("Throwable caught running FilterPump", t);
            setError();
        }
    }

    public String getChainName() {
        return chainName;
    }

    public String toString() {
        StringWriter sw = new StringWriter(500);
        sw.append(getStatus().toString()).append(": ");
        for (StreamFilter sf: streamFilters) {
            sw.append(sf.getClass().toString()).append(" => ");
        }
        for (RecordFilter rf: recordFilters) {
            sw.append(rf.getClass().toString()).append(" => ");
        }
        sw.append("pump");
        return sw.toString();
    }
}
