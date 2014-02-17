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
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * An extension of {@link SearchClient} that pipes requests and responses
 * through an {@link InteractionAdjuster}.
 * The properties for SearchClient must be specified as usual, along with the
 * properties for the InteractionAdjuster.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AdjustingSearchClient extends SearchClient {
    private static Log log = LogFactory.getLog(AdjustingSearchClient.class);

    private final InteractionAdjuster adjuster;
    private final TermStatQueryRewriter rewriter;
    private final String id;
    private final Profiler profiler = new Profiler(Integer.MAX_VALUE, 100);

    public AdjustingSearchClient(Configuration conf) {
        this(conf, null);
    }

    public AdjustingSearchClient(final Configuration conf, final TermStatQueryRewriter rewriter) {
        super(conf);
        adjuster = new InteractionAdjuster(conf);
        id = adjuster.getId();
        this.rewriter = rewriter;
        log.debug("Created " + this);
    }

    @Override
    public ResponseCollection search(Request request) throws IOException {
        final long startTime = System.currentTimeMillis();
        final String originalRequest = request.toString();
        ResponseCollection responses = null;
        boolean success = false;
        try {
            final String originalQuery = request.getString(DocumentKeys.SEARCH_QUERY, null);
            log.trace("Rewriting request for " + id + ", performing search and adjusting responses");
            long rewriteTime = -System.currentTimeMillis();
            Request adjusted = adjuster.rewrite(request); // Creates new request
            if (rewriter != null) {
                log.trace("Calling term stat based query rewriter with id " + adjuster.getId());
                rewriter.rewrite(adjusted, adjuster.getId());
            }
            final String finalQuery = adjusted.getString(DocumentKeys.SEARCH_QUERY, null);
            rewriteTime += System.currentTimeMillis();
            log.trace("Calling super.search");
            responses = super.search(adjusted);
            responses.setPrefix("adjuster_" + id + ".");
            log.trace("Adjusting response");
            long adjustTime = -System.currentTimeMillis();
            adjuster.adjust(adjusted, responses);
            adjustTime += System.currentTimeMillis();
            log.debug("Adjusted search for " + id + " with original query '" + originalQuery + "' and adjusted query '"
                      + finalQuery + " in " + (System.currentTimeMillis() - startTime) + " ms");
            responses.addTiming("request.rewrite", rewriteTime);
            responses.addTiming("response.adjust", adjustTime);
            responses.addTiming("total", System.currentTimeMillis() - startTime);
            success = true;
            return responses;
        } finally {
            profiler.beat();
            if (responses == null) {
                queries.info("AdjustingSearchClient finished "
                             + (success ? "successfully" : "unsuccessfully (see logs for errors)")
                              + " in " + (System.currentTimeMillis() - startTime)
                              + "ms. Request was " + originalRequest + ". " + getStats());
            } else {
                if (responses.getTransient() != null && responses.getTransient().containsKey(DocumentSearcher.DOCIDS)) {
                    Object o = responses.getTransient().get(DocumentSearcher.DOCIDS);
                    if (o instanceof DocIDCollector) {
                        ((DocIDCollector)o).close();
                    }
                }
                if (queries.isInfoEnabled()) {
                    String hits = "N/A";
                    for (Response response: responses) {
                        if (response instanceof DocumentResponse) {  // If it's there, we might as well get some stats
                            hits = Long.toString(((DocumentResponse)response).getHitCount());
                        }
                    }
                    queries.info("AdjustingSearchClient finished "
                                 + (success ? "successfully" : "unsuccessfully (see logs for errors)")
                                 + " in " + (System.currentTimeMillis() - startTime) + "ms with " + hits + " hits. "
                                 + "Request was " + originalRequest
                                 + " with Timing(" + responses.getTiming() + "). " + getStats());
                }
            }

        }
    }

    public InteractionAdjuster getAdjuster() {
        return adjuster;
    }

    @Override
    public String toString() {
        return "AdjustingSearchClient(id='" + id + "', adjuster=" + adjuster + ", rewriter=" + rewriter + ")";
    }

    private String getStats() {
        return "Stats(#queries=" + profiler.getBeats()
               + ", q/s(last " + profiler.getBpsSpan() + "=" + profiler.getBps(true) + ")";
    }

}
