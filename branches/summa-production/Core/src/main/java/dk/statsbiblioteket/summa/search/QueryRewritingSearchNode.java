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
import dk.statsbiblioteket.summa.search.tools.QueryPhraser;
import dk.statsbiblioteket.summa.search.tools.QuerySanitizer;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.queryparser.classic.ParseException;

import java.rmi.RemoteException;

/**
 * A search node wrapper that sanitizes queries and filters for common query syntax errors.
 * @see {@link dk.statsbiblioteket.summa.search.tools.QuerySanitizer} for details.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryRewritingSearchNode implements SearchNode {
    private static Log log = LogFactory.getLog(QueryRewritingSearchNode.class);

    /**
     * Whether or not incoming queries should be sanitized.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_SANITIZE_QUERIES = "rewriter.sanitizequeries";
    public static final String SEARCH_SANITIZE_QUERIES = CONF_SANITIZE_QUERIES;
    public static final boolean DEFAULT_SANITIZE_QUERIES = true;

    /**
     * Whether or not incoming filters should be sanitized.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_SANITIZE_FILTERS = "rewriter.sanitizefilters";
    public static final String SEARCH_SANITIZE_FILTERS = CONF_SANITIZE_FILTERS;
    public static final boolean DEFAULT_SANITIZE_FILTERS = true;

    /**
     * Whether or not to add a phrase query component to the query. This is primarily intended for searchers with a
     * simple query parser, such as Lucene's default. For more advanced query parsers that supports DisMax or
     * similar, no phrase should be added.
     * </p><p>
     * Optional. Default is false.
     * @see {@link dk.statsbiblioteket.summa.search.tools.QueryPhraser} for details.
     */
    public static final String CONF_PHRASE_QUERIES = "rewriter.phrasequeries";
    public static final String SEARCH_PHRASE_QUERIES = CONF_PHRASE_QUERIES;
    public static final boolean DEFAULT_PHRASE_QUERIES = false;

    /**
     * If specified, the designation can be used as a prefix for the search parameters to ensure that only the
     * specific QueryRewritingSearchNode receives the parameters.
     */
    public static final String CONF_DESIGNATION = "rewriter.designation";

    /**
     * A sub-configuration with the setup for the SearchNode that is to be created and used for all calls.
     * The configuration must contain the property
     * {@link dk.statsbiblioteket.summa.search.SearchNodeFactory#CONF_NODE_CLASS} as
     * {@link dk.statsbiblioteket.summa.search.SearchNodeFactory} is used for creating the single inner node.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_INNER_SEARCHNODE = "queryrewriter.inner.searchnode";

    private final boolean sanitizeQueries;
    private final boolean sanitizeFilters;
    private final boolean phrasequeries;
    private final SearchNode inner;
    private final QuerySanitizer sanitizer;
    private final QueryPhraser queryPhraser;
    private final String prefix;

    public QueryRewritingSearchNode(Configuration conf) {
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
        queryPhraser = new QueryPhraser(conf);
        phrasequeries = conf.getBoolean(CONF_PHRASE_QUERIES, DEFAULT_PHRASE_QUERIES);
        prefix = conf.valueExists(CONF_DESIGNATION) && !"".equals(conf.getString(CONF_DESIGNATION)) ?
                 conf.getString(CONF_DESIGNATION) + "." : "";
        log.debug("Created QueryRewritingSearchNode with inner SearchNode " + inner);
    }

    private Request process(Request request) {
        String oldQuery = request.getString(DocumentKeys.SEARCH_QUERY, null);
        if (oldQuery != null // TODO: Feedback should bubble to front end
            && request.getBoolean(prefix + SEARCH_SANITIZE_QUERIES,
                                  request.getBoolean(SEARCH_SANITIZE_QUERIES, sanitizeQueries))) {
            String newQuery = sanitizer.sanitize(oldQuery).getLastQuery();
            request.put(DocumentKeys.SEARCH_QUERY, newQuery);
            if (oldQuery.equals(newQuery)) {
                log.debug("Sanitized query is unchanged: '" + oldQuery + "'");
            } else {
                log.debug("Sanitized query '" + oldQuery + "' to '" + newQuery + "'");
            }
        }
        String oldFilter = request.getString(DocumentKeys.SEARCH_FILTER, null);
        if (oldFilter != null // TODO: Feedback should bubble to front end
            && request.getBoolean(prefix + SEARCH_SANITIZE_FILTERS,
                                  request.getBoolean(SEARCH_SANITIZE_FILTERS, sanitizeFilters))) {
            String newFilter = sanitizer.sanitize(oldFilter).getLastQuery();
            request.put(DocumentKeys.SEARCH_FILTER, newFilter);
            if (oldFilter.equals(newFilter)) {
                log.debug("Sanitized filter is unchanged: '" + oldFilter + "'");
            } else {
                log.debug("Sanitized filter '" + oldFilter + "' to '" + newFilter + "'");
            }
        }
        String query = request.getString(DocumentKeys.SEARCH_QUERY, null);
        if (query != null && request.getBoolean(prefix + SEARCH_PHRASE_QUERIES,
                                                request.getBoolean(SEARCH_PHRASE_QUERIES, phrasequeries))) {
            try {
                request.put(DocumentKeys.SEARCH_QUERY, queryPhraser.rewrite(request, query));
            } catch (ParseException e) {
                log.debug("The QueryPhraser threw a ParseException on '" + query
                          + "'. The query will be passed unmodified", e);
            }
        }
        return request;
    }

    /* Delegations below */

    @Override
    public void search(Request request, ResponseCollection responses) throws RemoteException {
        inner.search(process(request), responses);
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
