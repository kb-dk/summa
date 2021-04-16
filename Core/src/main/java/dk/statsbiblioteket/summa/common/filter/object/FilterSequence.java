/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wraps a sequence of ObjectFilters - in the Composite Pattern this would be a node.
 * </p><p>
 * The filters are specified by a list of subconfigurations. the key for the
 * list is {@link #CONF_FILTERS}. Each subconfiguration contains the class-name
 * of the wanted filter in the property {@link #CONF_FILTER_CLASS} along with
 * the filter-specific setup.
 * </p><p>
 * The filters are added in order of appearance and chained after each other.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "te")
// TODO: Make this an ObjectFilterImpl
public class FilterSequence extends ObjectFilterBase {
    private static Log log = LogFactory.getLog(FilterSequence.class);

    /**
     * A list of sub-configurations, one for each filter in the FilterSequence.
     * <p/>
     * This property is mandatory.
     */
    public static final String CONF_FILTERS = "summa.filter.sequence.filters";

    /**
     * The ObjectFilter class to instantiate.
     * </p><p>
     * This property is mandatory for each sub configuration in
     * {@link #CONF_FILTERS}.
     */
    public static final String CONF_FILTER_CLASS = "summa.filter.sequence.filterclass";

    /**
     * Whether or not the filter is active. If this is false for a filter,
     * the filter will not be part of the chain.
     * </p><p>
     * The use-case for this property is flexible configurations where parts
     * of the chain can be enabled or disabled via system properties.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_FILTER_ENABLED = "filter.enabled";
    public static final boolean DEFAULT_FILTER_ENABLED = true;

    private ArrayList<ObjectFilter> filters = new ArrayList<>(10);
    private ObjectFilter lastFilter = null;

    public FilterSequence(Configuration conf) throws IOException {
        super(conf);
        log.trace("Creating FilterSequence");
        List<Configuration> filterConfigurations;
        try {
            filterConfigurations = conf.getSubConfigurations(CONF_FILTERS);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException("Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
            List<String> filterNames;
            try {
                filterNames = conf.getStrings(CONF_FILTERS);
            } catch (Exception e2) {
                throw new ConfigurationException(String.format(
                        Locale.ROOT, "No Filters specified in property %s for FilterSequence", CONF_FILTERS), e);
            }
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "A list of Strings was specified in the property %s. A list of xproperties with filter-setups was "
                    + "expected. Maybe an old configuration-file hasn't been updated to the list-of-xproperties style? "
                    + "Encountered Strings was %s",
                    CONF_FILTERS, Logs.expand(filterNames, 10)), e);
        }
        buildChain(filterConfigurations);
        if (lastFilter == null) {
            throw new ConfigurationException("No filters created in FilterSequence");
        }
        // We only want output size
        setStatsDefaults(conf, false, false, false, true);
        log.debug("Finished building sequence  of " + filters.size() + " length");
    }

    private void buildChain(List<Configuration> filterConfigurations) throws IOException {
        log.trace("Entering buildChain");
        if (filterConfigurations == null) {
            log.warn("buildChain: No filter configurations");
            return;
        }
        Logs.log(log, Logs.Level.DEBUG, "Building filter sequence with " + filterConfigurations.size() + " filters");
        for (Configuration filterConf : filterConfigurations) {
            try {
                if (!filterConf.getBoolean(CONF_FILTER_ENABLED, DEFAULT_FILTER_ENABLED)) {
                    //noinspection DuplicateStringLiteralInspection
                    log.debug(String.format(Locale.ROOT, "Skipping %s filter of class %s as it is not enabled",
                                            filterConf.getString(Filter.CONF_FILTER_NAME, "unknown"),
                                            filterConf.getString(CONF_FILTER_CLASS, "unknown")));
                    continue;
                }
                ObjectFilter filter = createFilter(filterConf);
                log.debug("Adding Filter '" + filter + "' to sequence");
                if (lastFilter != null) {
                    log.trace("Chaining '" + filter + "' to the end of '" + lastFilter + "'");
                    filter.setSource(lastFilter);
                }
                lastFilter = filter;
                filters.add(lastFilter);
            } catch (Exception e) {
                throw new IOException(String.format(Locale.ROOT, "Could not create filter '%s'", filterConf), e);
            }
        }
        if (filters.isEmpty()) {
            log.warn("buildChain: No filters created");
        }
        log.info("Finished buildChain with " + filters.size() + " filters created");
    }

    private ObjectFilter createFilter(Configuration configuration) {
        Class<? extends ObjectFilter> filter = configuration.getClass(CONF_FILTER_CLASS, ObjectFilter.class);
        log.debug("Got filter class " + filter + ". Commencing creation");
        return Configuration.create(filter, configuration);
    }

    /**
     * @return a shallow copy of the list of the filters in this pump.
     */
    public List<Filter> getFilters() {
        return new ArrayList<Filter>(filters);
    }

    /* ObjectFilter interface */

    @Override
    public boolean hasNext() {
        return lastFilter.hasNext();
    }

    @Override
    public Payload next() {
        // TODO: Is it possible to differentiate between processing time and source pull time?
        final long startNS = System.nanoTime();
        Payload next =lastFilter.next();
        sizeProcess.process(next);
        logProcess(next, System.nanoTime()-startNS);
        logStatusIfNeeded();
        return next;
    }


    @Override
    public void setSource(Filter filter) {
        filters.get(0).setSource(filter);
    }

    @Override
    public boolean pump() throws IOException {
        return lastFilter.pump();
    }

    @Override
    public void close(boolean success) {
        super.close(success);
        lastFilter.close(success);
    }

    @Override
    public void remove() {
        lastFilter.remove();
    }

    @Override
    public String toString() {
        return "FilterSequence(filters=" + (filters == null ? "not_created_yet" : Strings.join(filters, ", ")) + ')';
    }
}
