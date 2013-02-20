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
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexField;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.util.SimplePair;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.browse.FacetRequest;
import dk.statsbiblioteket.summa.facetbrowser.browse.IndexLookup;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.exposed.ExposedCache;
import org.apache.lucene.search.exposed.ExposedRequest;
import org.apache.lucene.search.exposed.compare.ComparatorFactory;
import org.apache.lucene.search.exposed.compare.NamedComparator;
import org.apache.lucene.search.exposed.compare.NamedOrderDefaultComparator;
import org.apache.lucene.search.exposed.facet.CollectorPool;
import org.apache.lucene.search.exposed.facet.CollectorPoolFactory;
import org.apache.lucene.search.exposed.facet.FacetResponse;
import org.apache.lucene.search.exposed.facet.TagCollector;
import org.apache.lucene.search.exposed.facet.request.FacetRequestGroup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The implementation relies on
 * https://issues.apache.org/jira/browse/LUCENE-2369
 * for all heavy lifting regarding faceting.
 * </p><p>
 * The faceting system must be located after a SearchNode that updates the
 * response.transient data with a Lucene IndexSearcher {@code INDEX_SEARCHER}
 * and a SummaQueryParser {@code QUERY_PARSER}.
 * </p><p>
 * Note that this node is also responsible for index lookups. The configuration
 * for the node should thus contain settings for
 * {@link dk.statsbiblioteket.summa.facetbrowser.browse.IndexRequest} along
 * with settings for the facets.
 *
 * For query-syntax see:
 * @see dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys
 * @see dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class FacetSearchNode extends SearchNodeImpl implements Browser {
    private static Logger log = Logger.getLogger(FacetSearchNode.class);

    public static final String INDEX_SEARCHER = "INDEX_SEARCHER";
    public static final String QUERY_PARSER = "QUERY_PARSER";

    /**
     * Each concurrent search costs total number of tags * 4 bytes of memory.
     */
    public static final int DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES = 1;
    public static final int BROWSER_THREAD_QUEUE_TIMEOUT = 20 * 1000;
    //private static final long BROWSER_THREAD_MARK_TIMEOUT = 30 * 1000;

    public static final String CONF_COLLECTOR_POOLS = "exposed.collectorpoolfactory.collectorpools";
    public static final int DEFAULT_COLLECTOR_POOLS = 12;

    public static final String CONF_COLLECTOR_FILLED = "exposed.collectorpoolfactory.filledcollectors";
    public static final int DEFAULT_COLLECTOR_FILLED = 2;

    public static final String CONF_COLLECTOR_FRESH = "exposed.collectorpoolfactory.freshcollectors";
    public static final int DEFAULT_COLLECTOR_FRESH = 2;

    // Really ugly as this is indirectly shared with IndexLookup
    private static CollectorPoolFactory poolFactory;

    private Structure structure = null;

    private IndexLookup indexLookup;

    /**
     * The structure and facetMaps are updated upon open, depending on setup.
     * A lock is needed to ensure consistency.
     */
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private IndexSearcher searcher = null;
    private SummaQueryParser qp = null;

    private boolean loadDescriptorFromIndex;

    public FacetSearchNode(Configuration conf) throws RemoteException {
        super(conf);
        log.info("Constructing FacetSearchNode");
        poolFactory = new CollectorPoolFactory(
            conf.getInt(CONF_COLLECTOR_POOLS,  DEFAULT_COLLECTOR_POOLS),
            conf.getInt(CONF_COLLECTOR_FILLED, DEFAULT_COLLECTOR_FILLED),
            conf.getInt(CONF_COLLECTOR_FRESH,  DEFAULT_COLLECTOR_FRESH));
        // TODO: Add override-switch to state where to get the descriptor
        loadDescriptorFromIndex = !Structure.isSetupDefinedInConfiguration(conf);
        if (loadDescriptorFromIndex) {
            log.debug("The Structure will be derived from IndexDescriptor XML in the index folders upon calls to "
                      + "open(...)");
        } else {
            log.info(String.format(
                "The property %s was defined, so the IndexDescriptor will not be taken from the index-folder. "
                + "Note that this makes it hard to coordinate major updates to the IndexDescriptor in a production "
                + "system",
                IndexDescriptor.CONF_DESCRIPTOR));
            Structure structure = new Structure(conf);
            initStructures(structure);
        }
        indexLookup = new IndexLookup(conf);
        log.trace("FacetSearchNode constructed. Awaiting open");
    }

    /**
     * Fetches a descriptor from the file system. If there is currently no
     * descriptor or if the facets in the new descriptor differs from those in
     * the old descriptor, a write lock is acquired and the underlying structure
     * for the Facet searcher is initialized. The searcher is unavailable during
     * this phase.
     * </p><p>
     * If the new descriptor defines the same faces as the existing descriptor,
     * the secondary parameters (maximum tags and similar) are transfered. This
     * is a light-weight operation.
     * @param location the directory containing IndexDescriptor XML.
     * @throws RemoteException if the facet structure could not be created
     *         based on the IndexDescriptor.
     */
    private void updateDescriptor(File location) throws RemoteException {
        log.trace("Opening structure from '" + location + "'");
        if (location == null) {
            log.warn("Got null as location in updateDescriptor. Ignoring");
            return;
        }
        URL urlLocation = Resolver.getURL(location + "/" + IndexDescriptor.DESCRIPTOR_FILENAME);
        Structure newStructure = new Structure(urlLocation);
        if (structure != null && structure.absorb(newStructure)) {
            log.debug(String.format(
                "Only minor facet changes detected in the IndexDescriptor from '%s'. Major re-initialization skipped",
                urlLocation));
            return;
        }
        log.info(String.format("Performing major initialization based on '%s'",
                               urlLocation));
        initStructures(newStructure);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                "Facets after updateDescription(%s): %s",
                location, Strings.join(structure.getFacetNames(), ", ")));
        }
        indexLookup.updateDescriptor(location);
        updateConcats(urlLocation);
    }

    private void updateConcats(URL location) throws RemoteException {
        LuceneIndexDescriptor descriptor;
        try {
            descriptor = new LuceneIndexDescriptor(location);
        } catch (IOException e) {
            throw new RemoteException("Unable to load descriptor from '" + location + "'", e);
        }
        for (Map.Entry<String, LuceneIndexField> entry: descriptor.getFields().entrySet()) {
            LuceneIndexField field = entry.getValue();
            if (field.getParent() == null) {
                log.warn("No parent for field '" + field.getName() + "'");
                continue;
            }
            if (LuceneIndexDescriptor.COLLATED_DA.equals(entry.getValue().getParent().getName())) { // Giant hack, sorry
                String concatField = entry.getValue().getName();
                log.debug("Assigning " + concatField + " as concat field to ExposedCache");
                ExposedCache.getInstance().addConcatField(concatField);
            }
        }
    }


    private void initStructures(Structure newStructure) throws RemoteException {
        lock.writeLock().lock();
        try {
            structure = newStructure;
            // TODO: Consider clearing TagCounter cache
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void open(File directory) throws IOException {
        super.open(directory.toString());
    }

    @Override
    protected void managedWarmup(String request) {
        log.trace("managedWarmup(" + request + ") called. No effect for FacetSearchNode as it relies on previously "
                  + "chained DocumentSearcher");
    }

    @Override
    protected void managedClose() throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close() called");
        CollectorPoolFactory.getLastFactory().clear();
    }

    @Override
    protected void managedOpen(String location) throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("managedOpen(" + location + ") called");
        if (loadDescriptorFromIndex) {
            updateDescriptor(new File(location));
        }
    }

    // We always require a searcher and a query parser to ensure that the
    // searcher and the faceter are in sync.
    @Override
    protected void managedSearch(Request request, ResponseCollection responses) throws RemoteException {
        long startTime = System.currentTimeMillis();
        Map<String, Object> shared = responses.getTransient();
        DocIDCollector collectedIDs = assignShared(shared);

        indexLookup.lookup(request, responses);
       // handleExposedDirect(request, responses);

        // TODO: Construct a pseudo-query from query+filter+? Remember to remove setMaxFilled(0) when query is enabled
        String query = null;
        if (collectedIDs == null) {
            if (!request.containsKey(DocumentKeys.SEARCH_QUERY)) {
                log.trace("There were neither '" + DocumentSearcher.DOCIDS + ", nor '" + DocumentSearcher.SEARCH_QUERY
                         + " defined in the request, so no faceting could be performed");
                return;
            }
            // TODO: Add docID-agnostic faceting option
            log.debug("There were no docIDs collected for " + DocumentSearcher.SEARCH_QUERY
                      + "='" + request.get(DocumentKeys.SEARCH_QUERY) + "' so faceting is skipped");
            return;
//            log.debug("Faceting with query " + DocumentSearcher.SEARCH_QUERY
//                      + "='" + request.get(DocumentKeys.SEARCH_QUERY) + "'");
//            query = (String)shared.get(DocumentKeys.SEARCH_QUERY);
        } else {
            if (!request.containsKey(DocumentKeys.SEARCH_QUERY)) {
                log.debug("Faceting with " + collectedIDs.getDocCount() + " documents IDs");
            } else {
                log.debug("Both query '" + request.get(DocumentKeys.SEARCH_QUERY) + "' and "
                          + collectedIDs.getDocCount() + " doc IDs with key " + DocumentSearcher.DOCIDS
                          + " were defined in the query. Using document IDs");
            }
        }

        if (searcher == null) {
            throw new RemoteException(
                "No searcher defined. An IndexSearcher needs to be passed to this search node from a previous search "
                + "node in order to make faceting work");
        }
        org.apache.lucene.search.exposed.facet.request.FacetRequest facetRequest =
            constructFacetRequest(request, query);

        SimplePair<CollectorPool, TagCollector> pair;
        try {
            pair = PoolFactoryGate.acquire(poolFactory, searcher.getIndexReader(), query, facetRequest, "facet");
            pair.getKey().setMaxFilled(0);
        } catch (IOException e) {
            throw new RemoteException("Unable to acquire TagCollector for " + facetRequest, e);
        }
        CollectorPool collectorPool = pair.getKey();
        TagCollector tagCollector = pair.getValue();

        long collectTime = -System.currentTimeMillis();
        FacetResponse facetResponse;
        long extractTime;
        try {
            collect(tagCollector, query, collectedIDs);
            collectTime += System.currentTimeMillis();

            extractTime = -System.currentTimeMillis();
            facetResponse = tagCollector.extractResult(facetRequest);
            extractTime += System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException("Unable to extract response from TagCollector", e);
        } finally {
            PoolFactoryGate.release(collectorPool, tagCollector, query);
        }

        Response fr = newResponseToOldResult(facetResponse, request);
        fr.addTiming("facet.collectids.cached", collectTime);
        fr.addTiming("facet.extractfacets", extractTime);
        fr.addTiming("facet.total", System.currentTimeMillis() - startTime);
        responses.add(fr);
        if (log.isDebugEnabled()) {
            if (request.containsKey(DocumentKeys.SEARCH_QUERY)) {
                log.debug("Finished facet call for query '" + request.get(DocumentKeys.SEARCH_QUERY) + "' in "
                          + (System.currentTimeMillis() - startTime));
            } else {
                log.debug("Finished facet call for unknown query with " + collectedIDs.getDocCount() + " hits in "
                          + (System.currentTimeMillis() - startTime));
            }
        }

    }

    // TODO: Implement exposed direct call
/*    private void handleExposedDirect(
        Request request, ResponseCollection responses) throws RemoteException {
        if (!request.containsKey(FacetKeys.SEARCH_FACET_XMLREQUEST)) {
            return;
        }
        String r = request.getString(FacetKeys.SEARCH_FACET_XMLREQUEST);
        org.apache.lucene.search.exposed.facet.request.FacetRequest fRequest;
        try {
            fRequest = org.apache.lucene.search.exposed.facet.request.FacetRequest.parseXML(r);
        } catch (XMLStreamException e) {
            throw new RemoteException("Unable to parse exposed facet XML request'" + r + "'", e);
        }

//        assignShared(shared, collectedIDs);

    }*/

    // Not hierarchical
    private Response newResponseToOldResult(
        FacetResponse newResponse, Request request) {
        String facets = request.containsKey(FacetKeys.SEARCH_FACET_FACETS) ?
                        request.getString(FacetKeys.SEARCH_FACET_FACETS) :
                        null;
        // FIXME: The Structure should be derived from the request
        dk.statsbiblioteket.summa.facetbrowser.browse.FacetRequest oldFR = new FacetRequest(null, facets, structure);

        FacetResultExternal oldResult = new FacetResultExternal(
            oldFR.getMaxTags(), oldFR.getFacetIDs(), oldFR.getFacetFields(), structure);
        oldResult.setPrefix("");
        for (FacetResponse.Group group: newResponse.getGroups()) {
            String name = group.getRequest().getGroup().getName();
            for (FacetResponse.Tag tag: group.getTags().getTags()) {
                oldResult.addTag(name, tag.getTerm(), tag.getCount());
            }
        }
        return oldResult;
    }

    private void collect(TagCollector tagCollector, String query, DocIDCollector collectedIDs) {
        if (tagCollector.getQuery() == null) {
            log.trace("No cached tag collector. Performing collection");
            long collectTime = -System.currentTimeMillis();
            if (collectedIDs != null) {
                try {
                    tagCollector.collect(collectedIDs.getBits());
                } catch (IOException e) {
                    throw new RuntimeException(
                        "IOException while assigning previously collected IDs into TagCollector for query '"
                        + query + "'", e);
                }
                collectTime += System.currentTimeMillis();
                log.debug("Filled tagCollector from bit set with length " + collectedIDs.getBits().size() + " in "
                          + collectTime + " ms");
            } else {
                try {
                    // TODO: Support filter?
                    searcher.search(qp.parse(query), tagCollector);
                } catch (IOException e) {
                    throw new RuntimeException(
                        "IOException while collecting IDs into TagCollector for query '" + query + "'", e);
                } catch (ParseException e) {
                    throw new RuntimeException("Unable to parse query '" + query + "'", e);
                }
                collectTime += System.currentTimeMillis();
                log.debug("Filled tagCollector for query '" + query + "' in " + collectTime + " ms");
            }
        } else {
            log.debug("Previously filled tag collector was found for query '" + query + ". Skipping tag collection");
        }
    }

    private org.apache.lucene.search.exposed.facet.request.FacetRequest
    constructFacetRequest(Request request, String query) {
        String facets = request.containsKey(FacetKeys.SEARCH_FACET_FACETS) ?
                        request.getString(FacetKeys.SEARCH_FACET_FACETS) :
                        null;
        dk.statsbiblioteket.summa.facetbrowser.browse.FacetRequest oldFacetRequest = new FacetRequest(
                null, facets, structure);
        //ExposedRequest.

        List<FacetRequestGroup>
            groups = new ArrayList<FacetRequestGroup>();

        for (Map.Entry<String, FacetStructure> facet:
            oldFacetRequest.getFacets().entrySet()) {
            FacetStructure structure = facet.getValue();
            NamedComparator comparator;
            comparator = structure.getSortType().equals(FacetStructure.SORT_ALPHA) ?
                         ComparatorFactory.create(structure.getLocale()) :
                         new NamedOrderDefaultComparator();

            List<ExposedRequest.Field> fields = new ArrayList<ExposedRequest.Field>();
            for (String fieldName: structure.getFields()) {
                fields.add(new ExposedRequest.Field(fieldName, comparator));
            }
            // TODO: Add reverse to request and here
            ExposedRequest.Group group = new ExposedRequest.Group(structure.getName(), fields, comparator);
            FacetRequestGroup facetGroup = new FacetRequestGroup(
                group, comparator.getOrder(), false, structure.getLocale(),
                0, structure.getWantedTags(), 1, null);
            groups.add(facetGroup);
        }
        return new org.apache.lucene.search.exposed.facet.request.FacetRequest(query, groups);
    }

    private DocIDCollector assignShared(Map<String, Object> shared) throws RemoteException {
        Object o = shared.get(DocumentSearcher.DOCIDS);
        DocIDCollector collectedIDs = null;
        if (o != null) {
            if (!(o instanceof DocIDCollector)) {
                throw new RemoteException(String.format(
                    "Found transient data for key '%s'. Expected class %s, but got %s", DocumentSearcher.DOCIDS,
                    DocIDCollector.class.getName(), o.getClass().getName()));
            }
            collectedIDs = (DocIDCollector)o;
        }

        if (!shared.containsKey(INDEX_SEARCHER)) {
            log.error("The FacetSearchNode did not contain a value for key '" + INDEX_SEARCHER
                      + "'. No faceting can be performed. Please set the " + INDEX_SEARCHER
                      + " in the previous SearchNode");
            searcher = null;
        } else {
            IndexSearcher newSearcher = (IndexSearcher)shared.get(INDEX_SEARCHER);
            if (newSearcher != searcher) {
                log.info("Assigning new searcher");
                searcher = newSearcher;
            }
        }
        if (!shared.containsKey(QUERY_PARSER)) {
            if (collectedIDs != null) {
                log.debug("No " + QUERY_PARSER + " defined, but previously collected IDs were present so faceting will "
                          + "commence");
            } else {
                log.debug("The FacetSearchNode did not contain a value for key '" + QUERY_PARSER
                          + "'. No standard faceting can be performed");
            }
            qp = null;
        } else {
            SummaQueryParser newQP = (SummaQueryParser)shared.get(QUERY_PARSER);
            if (newQP != qp) {
                log.info("Assigning new query parser");
                qp = newQP;
            }
        }
        return collectedIDs;
    }

    @Override
    public List<String> getFacetNames() {
        return structure.getFacetNames();
    }

    @Override
    public FacetResult getFacetMap(DocIDCollector docIDs, String facets) throws RemoteException {
        lock.readLock().lock();
        try {
            FacetRequest facetRequest = new FacetRequest(docIDs, facets, structure);
            if (log.isDebugEnabled()) {
                log.debug("Got facet-request " + facetRequest.toString(false));
            }
            log.trace("Requesting BrowserThread");
        } finally {
            lock.readLock().unlock();
        }
        throw new UnsupportedOperationException("getFacetMap not implemented");
    }
}