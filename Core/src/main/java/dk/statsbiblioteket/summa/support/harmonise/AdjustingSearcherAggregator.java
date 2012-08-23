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
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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
 * It is the responsibility of the caller to ensure that the rewriter is
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

    private final ResponseMerger responseMerger;
    private TermStatQueryRewriter rewriter;

    public AdjustingSearcherAggregator(Configuration conf) {
        super(conf);
        responseMerger = new ResponseMerger(conf);
    }

    @Override
    protected void preConstruction(Configuration conf) {
        if (conf.valueExists(TermStatQueryRewriter.CONF_TARGETS)) {
            log.debug("Assigning term stat query rewriter");
            rewriter = new TermStatQueryRewriter(conf);
        } else {
            log.debug("No term stat query rewriter");
            rewriter = null;
        }
    }

    @Override
    protected SearchClient createClient(Configuration searcherConf) {
        SearchClient searcher;
        if (searcherConf.getBoolean(
            CONF_SEARCH_ADJUSTING, DEFAULT_SEARCH_ADJUSTING)) {
            log.debug("Creating adjusting search client with term stat based rewriter " + rewriter);
            searcher = new AdjustingSearchClient(searcherConf, rewriter);
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
