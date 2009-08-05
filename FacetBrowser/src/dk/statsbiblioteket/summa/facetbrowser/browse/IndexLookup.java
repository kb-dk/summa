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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexResponse;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.rmi.RemoteException;

/**
 * Utility class that performs IndexLookups on TagHandlers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexLookup {
    private static Log log = LogFactory.getLog(IndexLookup.class);

    private IndexRequest requestFactory;

    /**
     * Sets up a default {@link IndexRequest} based on the configuration.
     * @param conf the configuration for the index lookup.
     */
    public IndexLookup(Configuration conf) {
        log.debug("Constructing IndexLookup");
        requestFactory = new IndexRequest(conf);
    }

    /**
     * Parses the generic request into an IndexRequest and performs the lookup.
     * @param request    the lookup setup, as described in
     *             {@link dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys}.
     * @param response the response to the index lookup will be added to the
     *                 collection, if the request contained proper search keys
     *                 for an index lookup.
     * @param tagHandler aka the index.
     * @throws RemoteException if the field in the request did not match
     *         the tagHandler structure (aka there was no facet with that name).
     */
    public void lookup(Request request, ResponseCollection response,
                          TagHandler tagHandler) throws RemoteException {
        IndexRequest indexRequest = requestFactory.createRequest(request);
        if (indexRequest == null) {
            log.trace("No proper index lookup keys in the request");
            return;
        }
        response.add(lookup(indexRequest, tagHandler));
    }

    /**
     * Performs an index lookup with the given request and tag handler.
     * @param request    the field, term and other details on the index lookup.
     * @param tagHandler aka the index.
     * @return the result of an index lookup.
     * @throws IllegalStateException if the field in the request did not match
     *         the tagHandler structure (aka there was no facet with that name).
     */
    public static IndexResponse lookup(IndexRequest request,
                                       TagHandler tagHandler)
                                                  throws IllegalStateException {
        log.trace("lookup called");
        long startTime = System.nanoTime();
        int facetID = tagHandler.getFacetID(request.getField());
        if (facetID == -1) {
            throw new IllegalStateException(String.format(
                    "The wanted facet '%s' is not present in the tagHandler",
                    request.getField()));
        }
        int tagCount = tagHandler.getTagCount(request.getField());
        int origo = tagHandler.getNearestTagID(facetID, request.getTerm());
        origo = origo < 0 ? (origo + 1) * -1 : origo;
        int start = Math.max(0, origo + request.getDelta());

        IndexResponse response = new IndexResponse(
                request.getField(), request.getTerm(),
                request.isCaseSensitive(), request.getDelta(),
                request.getLength(), tagHandler.getFacetLocale(facetID));
        for (int tagID = start ;
             tagID < tagCount && tagID < start + request.getLength() ;
             tagID++) {
            response.addTerm(tagHandler.getTagName(facetID, tagID));
        }
        log.debug("Finished lookup for " + request.getField() + ":"
                  + request.getTerm() + " in "
                  + (System.nanoTime() - startTime) / 100000D + "ms");
        return response;
    }
}
