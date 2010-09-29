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
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.browse.FacetRequest;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The implementation uses threading and should scale well on a multi-processor
 * machine, if the architecture supports parallel RAM access. On a machine
 * with multiple processors, but old-style access to RAM, the increase in
 * performance is smaller.
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
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetSearchNode extends SearchNodeImpl implements Browser {
    private static Logger log = Logger.getLogger(FacetSearchNode.class);

    /**
     * Each concurrent search costs total number of tags * 4 bytes of memory.
     */
    public static final int DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES = 1;
    public static final int BROWSER_THREAD_QUEUE_TIMEOUT = 20 * 1000;
    private static final long BROWSER_THREAD_MARK_TIMEOUT = 30 * 1000;

    private Configuration conf;
    private Structure structure = null;
    /**
     * The structure and facetMaps are updated upon open, depending on setup.
     * A lock is needed to ensure consistency.
     */
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);


    private boolean loadDescriptorFromIndex;

    public FacetSearchNode(Configuration conf) throws RemoteException {
        super(conf);
        log.info("Constructing FacetSearchNode");
        this.conf = conf;
        // TODO: Add override-switch to state where to get the descriptor
        loadDescriptorFromIndex =
                !Structure.isSetupDefinedInConfiguration(conf);
        if (loadDescriptorFromIndex) {
            log.debug("The Structure will be derived from IndexDescriptor XML "
                      + "in the index folders upon calls to open(...)");
        } else {
            log.info(String.format(
                    "The property %s was defined, so the IndexDescriptor "
                    + "will not be taken from the index-folder. Note that this"
                    + " makes it hard to coordinate major updates to "
                    + "the IndexDescriptor in a production system",
                    IndexDescriptor.CONF_DESCRIPTOR));
            Structure structure = new Structure(conf);
            initStructures(structure);
        }
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
        URL urlLocation = Resolver.getURL(
                location + "/" + IndexDescriptor.DESCRIPTOR_FILENAME);
        Structure newStructure = new Structure(urlLocation);
        if (structure != null && structure.absorb(newStructure)) {
            log.debug(String.format(
                    "Only minor facet changes detected in the IndexDescriptor "
                    + "from '%s'. Major re-initialization skipped",
                    urlLocation));
            return;
        }
        log.info(String.format(
                "Performing major initialization based on '%s'", urlLocation));
        initStructures(newStructure);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Facets after updateDescription(%s): %s", 
                    location, Strings.join(structure.getFacetNames(), ", ")));
        }
    }

    private void initStructures(Structure newStructure) throws RemoteException {
        lock.writeLock().lock();
        try {
            structure = newStructure;
            throw new UnsupportedOperationException("init not done yet");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void open(File directory) throws IOException {
        super.open(directory.toString());
    }

    @Override
    protected void managedWarmup(String request) {
        log.trace("managedWarmup(" + request + ") called. No effect for "
                  + "FacetSearchNode as it relies on previously chained "
                  + "DocumentSearcher");
    }

    @Override
    protected void managedClose() throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close() called");
        throw new UnsupportedOperationException("close not done yet");
    }

    @Override
    protected void managedOpen(String location) throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("managedOpen(" + location + ") called");
        if (loadDescriptorFromIndex) {
            updateDescriptor(new File(location));
        }
        throw new UnsupportedOperationException("managedOpen not done yet");
    }

    @Override
    protected void managedSearch(Request request, ResponseCollection responses)
                                                        throws RemoteException {
        long startTime = System.currentTimeMillis();
//        indexLookup.lookup(request, responses, facetMap.getTagHandler());
        if (!responses.getTransient().containsKey(DocumentSearcher.DOCIDS)) {
            log.debug("No " + DocumentSearcher.DOCIDS + " from a previous "
                      + "DocumentSearcher in responses. Skipping faceting");
            return;
        }
        Object o = responses.getTransient().get(DocumentSearcher.DOCIDS);
        if (!(o instanceof DocIDCollector)) {
            throw new RemoteException(String.format(
                    "Found transient data for key '%s'. Expected class %s, "
                    + "but got %s", DocumentSearcher.DOCIDS,
                                    DocIDCollector.class.getName(),
                                    o.getClass().getName()));
        }
        DocIDCollector collector = (DocIDCollector)o;
        responses.add(getFacetMap(
                collector, request.containsKey(FacetKeys.SEARCH_FACET_FACETS) ?
                           request.getString(FacetKeys.SEARCH_FACET_FACETS) :
                           null));
        if (log.isDebugEnabled()) {
            if (request.containsKey(DocumentKeys.SEARCH_QUERY)) {
                log.debug("Finished facet call for query '"
                          + request.get(DocumentKeys.SEARCH_QUERY) + "' with "
                          + collector.getDocCount() + " hits in "
                          + (System.currentTimeMillis() - startTime));
            } else {
                log.debug("Finished facet call for unknown query with "
                          + collector.getDocCount() + " hits in "
                          + (System.currentTimeMillis() - startTime));
            }
        }
    }

    public List<String> getFacetNames() {
        return structure.getFacetNames();
    }

    public FacetResult getFacetMap(DocIDCollector docIDs, String facets) throws
                                                               RemoteException {
        lock.readLock().lock();
        try {
            FacetRequest facetRequest =
                    new FacetRequest(docIDs, facets, structure);
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