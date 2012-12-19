/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SummaSearcherAggregator;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.tools.QuerySanitizer;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.List;

/**
 * Optionally replaces standard SearchClients with AdjustingSearchClients.
 * Note that adjustments of individual search nodes must be enabled explicitly.
 * </p><p>
 * A {@link ResponseMerger} is attached automatically. Settings for the merger
 * must be provided in the configuration given to the constructor.
 * </p><p>
 * A {@link TermStatQueryRewriter} is attached if the key
 * {@link TermStatQueryRewriter#CONF_TARGETS} is present in properties and
 * the adjustment is enabled for the search client.
 * It is the responsibility of the caller to ensure that the adjuster is
 * configured with a target for each search client and that the ids os the
 * target matches the search clients.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AdjustingSearcherAggregator extends SummaSearcherAggregator {
    private static Log log = LogFactory.getLog(AdjustingSearcherAggregator.class);

    /**
     * Whether or not an {@link InteractionAdjuster} should be attached to
     * the remote searcher that is being constructed. Note that this setting
     * must be set for each SearchClient-configuration individually.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_SEARCH_ADJUSTING = "search.aggregator.searcher.adjusting";
    public static final boolean DEFAULT_SEARCH_ADJUSTING = false;

    /**
     * If true, queries are pre-processed by the {@link QuerySanitizer} before being passed on in the chain.
     * The QuerySanitizer can be tweaked by providing the properties defined in the class.
     * </p><p>
     * Optional. Default is true.    
     */
    public static final String CONF_SEARCH_SANITIZING = "search.aggregator.searcher.sanitizing";
    public static final boolean DEFAULT_SEARCH_SANITIZING = true;

    private final ResponseMerger responseMerger;
    private QuerySanitizer sanitizer;
    private TermStatQueryRewriter adjuster;

    public AdjustingSearcherAggregator(Configuration conf) {
        super(conf);
        responseMerger = new ResponseMerger(conf);
        if (conf.getBoolean(CONF_SEARCH_SANITIZING, DEFAULT_SEARCH_SANITIZING)) {
            sanitizer = new QuerySanitizer(conf);
            log.debug("QuerySanitizer enabled");
        } else {
            sanitizer = null;
        }
    }

    @Override
    protected void preProcess(Request request) {
        if (sanitizer != null) {
            {
                String query = request.getString(DocumentKeys.SEARCH_QUERY);
                if (query != null) {
                    QuerySanitizer.SanitizedQuery clean = sanitizer.sanitize(query);
                    log.debug("Sanitized '" + clean.getOriginalQuery() + "' -> '" + clean.getLastQuery());
                    request.put(DocumentKeys.SEARCH_QUERY, clean.getLastQuery());
                }
            }
            {
                String filter = request.getString(DocumentKeys.SEARCH_FILTER);
                if (filter!= null) {
                    QuerySanitizer.SanitizedQuery clean = sanitizer.sanitize(filter);
                    log.debug("Sanitized '" + clean.getOriginalQuery() + "' -> '" + clean.getLastQuery());
                    request.put(DocumentKeys.SEARCH_FILTER, clean.getLastQuery());
                }
            }
        }
    }

    @Override
    protected void preConstruction(Configuration conf) {
        if (conf.valueExists(TermStatQueryRewriter.CONF_TARGETS)) {
            log.debug("Assigning term stat query adjuster");
            adjuster = new TermStatQueryRewriter(conf);
        } else {
            log.debug("No term stat query adjuster");
            adjuster = null;
        }
    }

    @Override
    protected SearchClient createClient(Configuration searcherConf) {
        SearchClient searcher;
        if (searcherConf.getBoolean(
                CONF_SEARCH_ADJUSTING, DEFAULT_SEARCH_ADJUSTING)) {
            log.debug("Creating adjusting search client with term stat based adjuster " + adjuster);
            searcher = new AdjustingSearchClient(searcherConf, adjuster);
            String searcherName = searcherConf.getString(CONF_SEARCHER_DESIGNATION, searcher.getVendorId());
            String adjustID = ((AdjustingSearchClient)searcher).
                    getAdjuster().getId();
            if (!adjustID.equals(searcherName)) {
                throw new ConfigurationException(
                        "An AdjustingSearchClient was created with ID '" + adjustID + "' with an inner searcherID of '"
                        + searcherName + "'. Equal designations are required");
            }
            return searcher;
        }
        log.debug("Creating standard search client");
        return super.createClient(searcherConf);
    }

    @Override
    protected ResponseCollection merge(Request request, List<ResponseHolder> responses) {
        log.debug("Merging " + responses.size() + " responses");
        return responseMerger.merge(request, responses);
    }
}
