/**
 * Created: te 18-02-2008 23:59:10
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.filter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.StateThread;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sets up a chain of filters and pumps the last filter until no more data
 * can be retrieved.
 * The filters are specified with {@link #CONF_FILTERS} which contains an array
 * of keys for the configuration. For each key, a subConfiguration is retrieved.
 * The subConfiguration contains the class-name of the wanted filter in the
 * property {@link #CONF_FILTER_CLASS} along with the filter-specific setup.
 * </p><p>
 * The filters are added in order of appearance.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterPump extends StateThread implements Configurable {
    /* We delay the creation of the log until we know the name of the chain. */
    private Log log;
    private static Log classLog = LogFactory.getLog(FilterPump.class);

    public static final String CONF_FILTERS = "FilterPump.Filters";
    /**
     * The name of the chain is used for feedback and debugging.
     */
    public static final String CONF_CHAIN_NAME =   "FilterPump.ChainName";
    public static final String CONF_FILTER_CLASS = "FilterPump.FilterClass";

    private ArrayList<Filter> filters = new ArrayList<Filter>(10);
    private Filter lastFilter;

    private String chainName = "Unnamed Chain";

    long objectCounter = 0;
    long streamBytesCounter = 0;

    public FilterPump(Configuration configuration) throws IOException {
        classLog.trace ("Constructing FilterPump with config class "
                        + configuration.getClass());
        chainName = configuration.getString(CONF_CHAIN_NAME, chainName);
        classLog.trace ("Creating chain log for chain: " + chainName);
        log = LogFactory.getLog(FilterPump.class.getName() + "." + chainName);
        log.info("Constructing FilterPump for chain '" + chainName + "'");
        List<String> filterNames;
        try {
            filterNames = configuration.getStrings(CONF_FILTERS);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("No Filters specified in "
                                               + "property " + CONF_FILTER_CLASS
                                               + " for FilterPump");
        }

        buildChain(configuration, filterNames);
        log.debug("Finished building chain '" + chainName + "' of "
                  + filters.size() + " length");
    }

    private void buildChain(Configuration configuration,
                            List<String> filterNames) throws IOException {
        log.trace("Entering buildChain for '" + chainName + "'");
        if (filterNames != null) {
            Logs.log(log, Logs.Level.INFO, "Building filter chain with ",
                     filterNames);
            for (String filterName: filterNames) {
                log.debug("Adding Filter '" + filterName
                          + "' to chain");
                //noinspection OverlyBroadCatchBlock
                try {
                    Configuration filterConfiguration =
                            configuration.getSubConfiguration(filterName);
                    Filter filter = createFilter(filterConfiguration);
                    if (lastFilter != null) {
                        log.trace("Chaining '" + filter + "' to the end of '"
                                  + lastFilter + "'");
                        filter.setSource(lastFilter);
                    }
                    lastFilter = filter;
                    filters.add(lastFilter);
                } catch (Exception e) {
                    throw new IOException(String.format(
                            "Could not create filter '%s'",
                            filterName), e);
                }
            }
        }
        log.trace("Exiting buildChain for '" + chainName + "'");
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private Filter createFilter(Configuration configuration) {
        Class<? extends Filter> filter =
                configuration.getClass(CONF_FILTER_CLASS, Filter.class);
        log.debug("Got filter class " + filter + ". Commencing creation");
        return Configuration.create(filter, configuration);
    }

    // TODO: Better feedback with Profiler

    /**
     * The runMethod is normally managed by the {@link #start} and
     * {@link #stop} methods of FilterPump. It is not advisable to call it
     * explicitly.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    protected void runMethod() {
        log.debug("Running FilterChain '" + chainName + "'");
        try {
            long pumpActions = 0;
            while (getStatus() == STATUS.running) {
                if (!lastFilter.pump()) {
                    log.info(String.format("Finished pumping '%s' %d times",
                                           chainName, pumpActions));
                    break;
                }
                pumpActions++;
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
        }
    }

    public String getChainName() {
        return chainName;
    }

    public String toString() {
        StringWriter sw = new StringWriter(500);
        sw.append(getStatus().toString()).append(": ");
        for (Filter filterf: filters) {
            sw.append(filterf.getClass().toString()).append(" => ");
        }
        sw.append("pump");
        return sw.toString();
    }

    /**
     * @return a shallow copy of the list of the filters in this pump.
     */
    public List<Filter> getFilters() {
        return new ArrayList<Filter>(filters);
    }

}