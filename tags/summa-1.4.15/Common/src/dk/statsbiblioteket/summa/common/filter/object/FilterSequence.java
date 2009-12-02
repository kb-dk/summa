/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a sequence of ObjectFilters - in the Composite Pattern this would be a
 * node.
 * </p><p>
 * The filters are specified by a list of subconfigurations. the key for the
 * list is {@link #CONF_FILTERS}. Each subconfiguration contains the class-name
 * of the wanted filter in the property {@link #CONF_FILTER_CLASS} along with
 * the filter-specific setup.
 * </p><p>
 * The filters are added in order of appearance.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterSequence implements ObjectFilter {
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
    public static final String CONF_FILTER_CLASS =
            "summa.filter.sequence.filterclass";

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

    private ArrayList<ObjectFilter> filters = new ArrayList<ObjectFilter>(10);
    private ObjectFilter lastFilter = null;

    public FilterSequence(Configuration conf) throws IOException {
        log.trace("Creating FilterSequence");
        List<Configuration> filterConfigurations;
        try {
            filterConfigurations = conf.getSubConfigurations(CONF_FILTERS);
        } catch (IOException e) {
            List<String> filterNames;
            try {
                filterNames = conf.getStrings(CONF_FILTERS);
            } catch (Exception e2) {
                throw new ConfigurationException(String.format(
                        "No Filters specified in property %s for "
                        + "FilterSequence", CONF_FILTERS), e);
            }
            throw new ConfigurationException(String.format(
                    "A list of Strings was specified in the property %s. A "
                    + "list of xproperties with filter-setups was expected."
                    + " Maybe an old configuration-file hasn't been updated"
                    + " to the list-of-xproperties style? Encountered "
                    + "Strings was %s",
                    CONF_FILTERS, Logs.expand(filterNames, 10)), e);
        }
        buildChain(filterConfigurations);
        if (lastFilter == null) {
            throw new ConfigurationException(
                    "No filters created in FilterSequence");
        }
        log.debug("Finished building sequence  of " + filters.size()
                  + " length");
    }

    private void buildChain(List<Configuration> filterConfigurations)
                                                            throws IOException {
        log.trace("Entering buildChain");
        if (filterConfigurations == null) {
            log.warn("buildChain: No filter configurations");
            return;
        }
        Logs.log(log, Logs.Level.INFO, "Building filter sequence with "
                 + filterConfigurations.size() + " filters");
        for (Configuration filterConf: filterConfigurations) {
            try {
                if (!filterConf.getBoolean(
                        CONF_FILTER_ENABLED, DEFAULT_FILTER_ENABLED)) {
                    //noinspection DuplicateStringLiteralInspection
                    log.debug(String.format(
                            "Skipping %s filter of class %s as it is not "
                            + "enabled",
                            filterConf.getString(
                                    Filter.CONF_FILTER_NAME, "unknown"),
                            filterConf.getString(CONF_FILTER_CLASS, "unknown")
                    ));
                    continue;
                }
                ObjectFilter filter = createFilter(filterConf);
                log.debug("Adding Filter '" + filter + "' to sequence");
                if (lastFilter != null) {
                    log.trace("Chaining '" + filter + "' to the end of '"
                              + lastFilter + "'");
                    filter.setSource(lastFilter);
                }
                lastFilter = filter;
                filters.add(lastFilter);
            } catch (Exception e) {
                throw new IOException(String.format(
                        "Could not create filter '%s'", filterConf), e);
            }
        }
        if (filters.size() == 0) {
            log.warn("buildChain: No filters created");
        }
        log.info("Finished buildChain with " + filters.size()
                 + " filters created");
    }

    private ObjectFilter createFilter(Configuration configuration) {
        Class<? extends ObjectFilter> filter =
                configuration.getClass(CONF_FILTER_CLASS, ObjectFilter.class);
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

    public boolean hasNext() {
        return lastFilter.hasNext();
    }

    public void setSource(Filter filter) {
        filters.get(0).setSource(filter);
    }

    public boolean pump() throws IOException {
        return lastFilter.pump();
    }

    public void close(boolean success) {
        lastFilter.close(success);
    }

    public Payload next() {
        return lastFilter.next();
    }

    public void remove() {
        lastFilter.remove();
    }
}
