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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.facetbrowser.FacetIndexDescriptor;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexResponse;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.exposed.ExposedRequest;
import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.search.exposed.facet.CollectorPool;
import org.apache.lucene.search.exposed.facet.CollectorPoolFactory;
import org.apache.lucene.search.exposed.facet.FacetResponse;
import org.apache.lucene.search.exposed.facet.TagCollector;
import org.apache.lucene.search.exposed.facet.request.FacetRequest;
import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class that performs IndexLookups on TagHandlers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexLookup {
    private static Log log = LogFactory.getLog(IndexLookup.class);

    public static final String INDEX_SEARCHER = "INDEX_SEARCHER";
    public static final String QUERY_PARSER = "QUERY_PARSER";

    private IndexRequest requestFactory;
    private FacetIndexDescriptor descriptor = null;

    // TODO: Make a more common creator with custom values
    private static CollectorPoolFactory poolFactory =
        CollectorPoolFactory.getLastFactory() == null ?
        new CollectorPoolFactory(6, 2, 2) : 
        CollectorPoolFactory.getLastFactory();

    private IndexSearcher searcher = null;
    private SummaQueryParser qp = null;

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
     * @throws java.rmi.RemoteException if the field in the request did not match
     *         the tagHandler structure (aka there was no facet with that name).
     */
    public void lookup(Request request, ResponseCollection response)
                                                        throws RemoteException {
        if (descriptor == null) {
            log.warn("No descriptor defined in IndexLookup. "
                     + "Locale-based sorting will not be used ");
        }
        IndexRequest indexRequest = requestFactory.createRequest(request);
        if (indexRequest == null) {
            log.trace("No proper index lookup keys in the request");
            return;
        }
        Map<String, Object> shared = response.getTransient();
        if (!shared.containsKey(INDEX_SEARCHER)) {
            log.error("No IndexSearcher in responses transients. No index "
                      + "lookup will be performed");
            searcher = null;
            return;
        }
        IndexSearcher newSearcher = (IndexSearcher)shared.get(INDEX_SEARCHER);
        if (newSearcher != searcher) {
            log.info("Assigning new searcher");
            searcher = newSearcher;
        }
        if (!shared.containsKey(QUERY_PARSER)) {
            log.error("No QueryParser in responses transients. No index "
                      + "lookup will be performed");
            qp = null;
            return;
        }
        SummaQueryParser newQP = (SummaQueryParser)shared.get(QUERY_PARSER);
        if (newQP != qp) {
            log.info("Assigning new query parser");
            qp = newQP;
        }

        response.add(lookup(indexRequest));
    }

    // TODO: Add locale to request
    /**
     * Performs an index lookup with the given request and tag handler.
     * @param request    the field, term and other details on the index lookup.
     * @return the result of an index lookup.
     * @throws IllegalStateException if the field in the request did not match
     *         the tagHandler structure (aka there was no facet with that name).
     */
    private IndexResponse lookup(IndexRequest request)
                                                  throws IllegalStateException {
        log.trace("lookup called");
        long lookupTime = -System.currentTimeMillis();
        long collectTime = 0;
        long extractTime = 0;
        org.apache.lucene.search.exposed.facet.request.FacetRequest fRequest =
            createFacetRequest(request);

//        System.out.println("Requesting for\n" + fRequest.toXML());

        IndexReader reader = searcher.getIndexReader();
        CollectorPool collectorPool;
        try {
            if (!poolFactory.hasPool(searcher.getIndexReader(), fRequest)) {
                log.info("The CollectorPoolFactory has no structures for the given request. A new structure will be "
                         + "generated, which can take several minutes. The request was " + fRequest.getBuildKey()
                         + " with groupKey '" + fRequest.getGroupKey() + "'");
            }
            collectorPool = poolFactory.acquire(reader, fRequest);
        } catch (IOException e) {
            throw new RuntimeException(
                "Unable to acquire a CollectorPool for " + fRequest, e);
        }
        String queryKey = toQueryKey(request);
        TagCollector tagCollector = collectorPool.acquire(queryKey);
        if (queryKey.equals(tagCollector.getQuery())) {
            log.debug("Reusing already filled collector for " + queryKey);
        } else {
            collectTime = -System.currentTimeMillis();
            collect(tagCollector, fRequest, request.getQuery());
            collectTime += System.currentTimeMillis();
        }
        FacetResponse fResponse;
        try {
            extractTime = -System.currentTimeMillis();
            fResponse = tagCollector.extractResult(fRequest);
            extractTime += System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(
                "Unable to extract response from TagCollector", e);
        }
        collectorPool.release(queryKey, tagCollector);

        //System.out.println("Got response\n" + fResponse.toXML());
        
        IndexResponse iResponse = newResponseToOldResult(fResponse, request);
        lookupTime += System.currentTimeMillis();
        log.debug("Finished IndexLookup for " + request + " in " + lookupTime
                  + " ms");
        iResponse.addTiming("lookup.collectids", collectTime);
        iResponse.addTiming("lookup.extractfacets", extractTime);
        iResponse.addTiming("lookup.total", lookupTime);
        return iResponse;
    }

    private String toQueryKey(IndexRequest request) {
        return (request.getQuery() == null ? "*" : request.getQuery())
               + request.getField();
    }

    private IndexResponse newResponseToOldResult(
        FacetResponse fResponse, IndexRequest request) {
        IndexResponse response = new IndexResponse(request);
        for (FacetResponse.Tag tag:
            fResponse.getGroups().get(0).getTags().getTags()) {
            response.addTerm(new Pair<String, Integer>(
                tag.getTerm(), tag.getCount()));
        }
        return response;
    }

    private void collect(TagCollector tagCollector,
                         FacetRequest fRequest, String query) {
        query = query == null ? "*" : query;
        long collectTime = -System.currentTimeMillis();
        try {
            searcher.search(qp.parse(query), tagCollector);
        } catch (IOException e) {
            throw new RuntimeException(
                "IOException while collecting IDs into TagCollector "
                + "for query '" + query + "'", e);
        } catch (ParseException e) {
            throw new RuntimeException(
                "Unable to parse query '" + query + "'", e);
        }
        collectTime += System.currentTimeMillis();
        log.debug("Filled tagCollector for query '" + query + "' in "
                  + collectTime + " ms");
    }

    private org.apache.lucene.search.exposed.facet.request.FacetRequest
                                      createFacetRequest(IndexRequest request) {
        Locale locale = null;
        if (request.getLocale() != null) {
            locale = request.getLocale();
        } else if (descriptor != null && descriptor.getFacets() != null
                   && descriptor.getFacets().get(request.getField()) != null) {
            locale = new Locale(descriptor.getFacets().get(request.getField()).getLocale());
        }
        NamedComparator comparator = ComparatorFactory.create(locale);

        List<ExposedRequest.Field> fields =
            new ArrayList<ExposedRequest.Field>();
        // TODO: Add reverse to request and here
        fields.add(new ExposedRequest.Field(request.getField(), comparator));
        List<FacetRequestGroup> groups = new ArrayList<FacetRequestGroup>(1);
        // TODO: Add reverse to request and here
        ExposedRequest.Group eGroup = new ExposedRequest.Group(
            request.getField(), fields, comparator);
        NamedComparator.ORDER facetOrder = locale == null ?
                                           NamedComparator.ORDER.index :
                                           NamedComparator.ORDER.locale;

        // TODO: Add reverse to request and here
        FacetRequestGroup facetGroup = new FacetRequestGroup(
            eGroup, facetOrder, false,
            locale == null ? null : locale.toString(),
            request.getDelta(), request.getLength(), request.getMinCount(),
            request.getTerm());
        groups.add(facetGroup);
        org.apache.lucene.search.exposed.facet.request.FacetRequest fRequest =
            new org.apache.lucene.search.exposed.facet.request.FacetRequest(
            request.getQuery() == null ? "*" : request.getQuery(), groups);
        return fRequest;
    }

    public void updateDescriptor(File location) {
        URL descriptorLocation = Resolver.getURL(
            location + "/" + IndexDescriptor.DESCRIPTOR_FILENAME);
        try {
            descriptor = new FacetIndexDescriptor(descriptorLocation);
        } catch (IOException e) {
            log.warn("IOException while retrieving and parsing "
                     + "FacetIndexDescriptor in updateIndex. Lookups will not"
                     + " use locale-based sorting", e);
        }
        log.debug("Updated index descriptor from '" + location + "'");
    }
}