/* $Id:$
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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Wraps a sequence of ObjectFilters - in the Composite Pattern this would be a
 * node.
 * </p><p>
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
public class FilterSequence implements ObjectFilter {
    private static Log log = LogFactory.getLog(FilterSequence.class);

    /**
     * A list with the names of the filters to create and put in sequence.
     * For each name, a sub-configuration must exist.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_FILTERS = "summa.filter.sequence.filters";

    /**
     * The ObjectFilter class to instantiate.
     * </p><p>
     * This property is mandatory for each sub configuration stated in
     * {@link #CONF_FILTERS}.
     */
    public static final String CONF_FILTER_CLASS =
            "summa.filter.sequence.filterclass";

    private ArrayList<ObjectFilter> filters = new ArrayList<ObjectFilter>(10);
    private ObjectFilter lastFilter = null;

    public FilterSequence(Configuration conf) throws IOException {
        log.trace("Creating FilterSequence");
        List<String> filterNames;
        try {
            filterNames = conf.getStrings(CONF_FILTERS);
        } catch (NullPointerException e) {
            throw new ConfigurationException(String.format(
                    "No Filters specified in property %s for FilterSequence",
                    CONF_FILTERS), e);
        }

        buildChain(conf, filterNames);
        if (lastFilter == null) {
            throw new ConfigurationException(
                    "No filters created in FilterSequence");
        }
        log.debug("Finished building sequence  of " + filters.size()
                  + " length");
    }

    private void buildChain(Configuration configuration,
                            List<String> filterNames) throws IOException {
        log.trace("Entering buildChain");
        if (filterNames != null) {
            Logs.log(log, Logs.Level.INFO, "Building filter sequence with ",
                     filterNames);
            for (String filterName: filterNames) {
                log.debug("Adding Filter '" + filterName + "' to sequence");
                //noinspection OverlyBroadCatchBlock
                try {
                    Configuration filterConfiguration =
                            configuration.getSubConfiguration(filterName);
                    ObjectFilter filter = createFilter(filterConfiguration);
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
        log.trace("Exiting buildChain");
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
