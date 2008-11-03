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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.List;

/**
 * The MUXFilter divides incoming Payloads among one or more ObjectFilters
 * based on {@link dk.statsbiblioteket.summa.common.Record#getBase()}.
 * The Payloads must contain a Record.
 * </p><p>
 * One straight forward use case is to mux several {@link XMLTransformer}s,
 * each responsible for handling a specific base.
 * </p><p>
 * The MUXFilter is - as all ObjectFilters - pull-based. There is a single
 * sink that pulls all contained Filters in parallel. Each filter gets Payloads
 * from a {@link MUXFilterFeeder} that in turn gets Payloads from the MUXFilter.
 * In order to provide hasNext()/next()-consistent behaviour (e.g. that there
 * always is a next(), is hasNext() is true), the MUXFilterFeeders might at any
 * time contain 1 cached Payload. 
 */
// TODO: Make a FilterProxy and a FilterSequencer.
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MUXFilter {
    private static Log log = LogFactory.getLog(MUXFilter.class);

    /**
     * A list of Strings specifying the subProperties for the Filters to mux.
     * Note that Filters may appear more than once in the list, in which case
     * more instances are created. Multiple instances of the same Filter might
     * be used together with ProxyFilters to provide Threaded execution.
     * </p><p>
     * For each unique Filter specified in this property, a subConfiguration
     * must exist in the Configuration.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_FILTERS = "summa.muxfilter.filters";

    /**
     * The Class name for the Filter to use if no other filters accepts an
     * incoming Payload.
     * </p><p>
     * This property is optional. If not defined, a warning is logged and the
     * non-fitting Payload is discarded.
     */
    public static final String CONF_DEFAULT_FILTER =
            "summa.muxfilter.defaultfilter";
    /**
     * The Class name for a filter specified in {@link #CONF_FILTERS}.
     * A new Filter will be created from this Class by introspection
     * and used by the muxer.
     */
    public static final String CONF_FILTER_CLASS =
            "summa.muxfilter.filter.class";
    /**
     * A list of Strings specifying the bases that the Filter accepts.
     * "*" specifies that all bases are accepted.
     * </p><p>
     * This property is mandatory for all Filters except the default Filter.
     */
    public static final String CONF_FILTER_NAME =
            "summa.muxfilter.filter.bases";

    private ObjectFilter defaultFilter = null;

    public MUXFilter(Configuration conf) {
        log.debug("Constructing MUXFilter");
        if (!conf.valueExists(CONF_FILTERS)) {
            throw new Configurable.ConfigurationException(String.format(
                    "A value for the key %s must exist in the Configuration",
                    CONF_FILTERS));
        }
        List<String> filterNames = conf.getStrings(CONF_FILTERS);
        String defaultFilterName = conf.getString(CONF_DEFAULT_FILTER, null);
        if (defaultFilter != null) {
            log.debug("Creating default filter '" + defaultFilterName);
        }
//        for (String filterName)
    }
}
