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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.tools.QuerySanitizer;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.rmi.RemoteException;

/**
 * A search node wrapper that sanitizes queries and filters for common query syntax errors.
 * @see {@link dk.statsbiblioteket.summa.search.tools.QuerySanitizer} for details.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SanitizingSearchNode implements SearchNode {
    private static Log log = LogFactory.getLog(SanitizingSearchNode.class);

    /**
     * Whether or not incoming queries should be sanitized.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_SANITIZE_QUERIES = "sanitizer.sanitizequeries";
    public static final String SEARCH_SANITIZE_QUERIES = CONF_SANITIZE_QUERIES;
    public static final boolean DEFAULT_SANITIZE_QUERIES = true;

    /**
     * Whether or not incoming filters should be sanitized.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_SANITIZE_FILTERS = "sanitizer.sanitizefilters";
    public static final String SEARCH_SANITIZE_FILTERS = CONF_SANITIZE_FILTERS;
    public static final boolean DEFAULT_SANITIZE_FILTERS = true;

    /**
     * A sub-configuration with the setup for the SearchNode that is to be created and used for all calls.
     * The configuration must contain the property
     * {@link dk.statsbiblioteket.summa.search.SearchNodeFactory#CONF_NODE_CLASS} as
     * {@link dk.statsbiblioteket.summa.search.SearchNodeFactory} is used for creating the single inner node.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_INNER_SEARCHNODE = "sanitizer.inner.searchnode";

    private final boolean sanitizeQueries;
    private final boolean sanitizeFilters;
    private final SearchNode inner;
    private final QuerySanitizer sanitizer;

    public SanitizingSearchNode(Configuration conf) {
        if (!conf.valueExists(CONF_INNER_SEARCHNODE)) {
            throw new ConfigurationException(
                "No inner search node defined. A proper sub-configuration must exist for key " + CONF_INNER_SEARCHNODE);
        }
        try {
            inner = SearchNodeFactory.createSearchNode(conf.getSubConfiguration(CONF_INNER_SEARCHNODE));
        } catch (RemoteException e) {
            throw new ConfigurationException(
                "Unable to create inner search node, although a value were present for key " + CONF_INNER_SEARCHNODE);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException(
                "A configuration with support for sub configurations must be provided for the adjuster and must "
                + "contain a sub configuration with key " + CONF_INNER_SEARCHNODE);
        }
        sanitizer = new QuerySanitizer(conf);
        sanitizeQueries = conf.getBoolean(CONF_SANITIZE_QUERIES, DEFAULT_SANITIZE_QUERIES);
        sanitizeFilters = conf.getBoolean(CONF_SANITIZE_FILTERS, DEFAULT_SANITIZE_FILTERS);
        log.debug("Created SanitizingSearchNode with inner SearchNode " + inner);
    }

    private Request sanitize(Request request) {
        String oldQuery = request.getString(DocumentKeys.SEARCH_QUERY, null);
        String oldFilter = request.getString(DocumentKeys.SEARCH_FILTER, null);
        if (oldQuery != null
            && request.getBoolean(SEARCH_SANITIZE_QUERIES, sanitizeQueries)
            && request.containsKey(DocumentKeys.SEARCH_QUERY)) { // TODO: Feedback should bubble to front end
            String newQuery = sanitizer.sanitize(oldQuery).getLastQuery();
            request.put(DocumentKeys.SEARCH_QUERY, newQuery);
            log.debug("Sanitized query '" + oldQuery + "' to '" + newQuery + "'");
        }
        if (oldFilter != null
            && request.getBoolean(SEARCH_SANITIZE_QUERIES, sanitizeFilters)
            && request.containsKey(DocumentKeys.SEARCH_FILTER)) { // TODO: Feedback should bubble to front end
            String newFilter = sanitizer.sanitize(oldFilter).getLastQuery();
            request.put(DocumentKeys.SEARCH_FILTER, newFilter);
            log.debug("Sanitized Filter '" + oldFilter + "' to '" + newFilter + "'");
        }
        return request;
    }

    /* Delegations below */

    @Override
    public void search(Request request, ResponseCollection responses) throws RemoteException {
        inner.search(sanitize(request), responses);
    }

    @Override
    public void warmup(String request) {
        inner.warmup(request);
    }

    @Override
    public void open(String location) throws RemoteException {
        inner.open(location);
    }

    @Override
    public void close() throws RemoteException {
        inner.close();
    }

    @Override
    public int getFreeSlots() {
        return inner.getFreeSlots();
    }
}
