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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: FacetSearchNode.java,v 1.14 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.browse.BrowserThread;
import dk.statsbiblioteket.summa.facetbrowser.browse.FacetRequest;
import dk.statsbiblioteket.summa.facetbrowser.browse.IndexLookup;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerFactory;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
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
 * @see {@link FacetKeys} and
 * {@link dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys} for 
 * query-syntax.
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
    private FacetMap facetMap = null;
    private IndexLookup indexLookup;
    /**
     * The structure and facetMaps are updated upon open, depending on setup.
     * A lock is needed to ensure consistency.
     */
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);


    private boolean loadDescriptorFromIndex;

    private BlockingQueue<BrowserThread> browsers;

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
            long startTime = System.currentTimeMillis();
            structure = newStructure;
            if (facetMap != null) {
                log.debug("Closing existing facet map");
                facetMap.close();
            }
            TagHandler tagHandler = TagHandlerFactory.getTagHandler(
                    conf, structure, true);
            CoreMap coreMap = CoreMapFactory.getCoreMap(conf, structure);
            facetMap = new FacetMap(structure, coreMap, tagHandler, true);
            browsers = new ArrayBlockingQueue<BrowserThread>(
                    getMaxConcurrentSearches(), true);
            for (int i = 0 ; i < getMaxConcurrentSearches() ; i++) {
                try {
                    browsers.put(new BrowserThread(tagHandler, coreMap));
                } catch (InterruptedException e) {
                    throw new RemoteException("Interrupted while trying to add"
                                              + " BrowserThread to queue");
                }
            }
            log.debug(String.format(
                    "Finished initalizing in %d ms",
                    System.currentTimeMillis() - startTime));
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
        if (facetMap != null) {
            facetMap.close();
        }
    }

    @Override
    protected void managedOpen(String location) throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("managedOpen(" + location + ") called");
        if (loadDescriptorFromIndex) {
            updateDescriptor(new File(location));
        }
        try {
            Profiler profiler = new Profiler();
            facetMap.open(new File(location, FacetCore.FACET_FOLDER));
            log.debug(String.format(
                    "managedOpen(%s) finished in %s",
                    location, profiler.getSpendTime()));
        } catch (IOException e) {
            throw new RemoteException(String.format(
                    "Unable to open facetMap at location '%s'", location));
        }
    }

    @Override
    protected void managedSearch(Request request, ResponseCollection responses)
                                                        throws RemoteException {
        long startTime = System.currentTimeMillis();
        if (facetMap == null) {
            throw new RemoteException("Unable to perform facet search as no "
                                      + "facet structure has been opened");
        }
        indexLookup.lookup(request, responses, facetMap.getTagHandler());
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
            BrowserThread browserThread;
            try {
                browserThread = browsers.poll(
                        BROWSER_THREAD_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RemoteException("Interrupted while waiting for a "
                                          + "free BrowserThread", e);
            }
            if (browserThread == null) {
                throw new RemoteException(String.format(
                        "Timeout after %d ms while waiting for a free "
                        + "BrowserThread", BROWSER_THREAD_QUEUE_TIMEOUT));
            }
            try {
                // TODO: Make this threaded
                log.trace("Activating BrowserThread");
                browserThread.startRequest(
                        docIDs, 0, (int)docIDs.getBits().capacity(), 
                        facetRequest);
                log.trace("Waiting for browserThread");
                browserThread.waitForResult(BROWSER_THREAD_MARK_TIMEOUT);
                log.trace("Finished waiting for BrowserThread, "
                          + "returning result");
                return browserThread.getResult().externalize();
            } finally {
                while (true) {
                    try {
                        browsers.put(browserThread);
                        break;
                    } catch (InterruptedException e) {
                        log.debug("Interrupted while putting browserThread. "
                                  + "Retrying");
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        /*
    protected synchronized FacetResult getFacetMap(String query,
                                       FacetResult.TagSortOrder sortOrder,
                                       SlimCollector slimCollector) {

        int threadCount =
                slimCollector.getDocumentCount() < SINGLE_THREAD_THRESHOLD ?
                1 : browsers.size();
        int partSize = slimCollector.getDocumentCount() / threadCount;
        int startPos = 0;
        for (int browserID = 0 ; browserID < threadCount ; browserID++) {
            BrowserThread browser = browsers.get(browserID);
            browser.startRequest(slimCollector.getDocumentIDsOversize(),
                                 startPos,
                                 browserID == threadCount-1 ?
                                 slimCollector.getDocumentCount()-1 :
                                 startPos + partSize-1, sortOrder);
            startPos += partSize;
        }

        FacetResult structure = null;
        for (int browserID = 0 ; browserID < threadCount ; browserID++) {
            BrowserThread browser = browsers.get(browserID);
            browser.waitForResult(timeout);
            if (browser.hasFinished()) {
                if (structure == null) {
                    structure = browser.getResult();
                } else {
                    structure.merge(browser.getResult());
                }
            } else {
                log.error("Skipping browser thread #" + browserID
                          + " as it has not finished. The facet/tag result "
                          + "will not be correct");
            }
        }
        if (structure != null) {
            structure.reduce(sortOrder);
        }
        log.debug("Created FacetResult from query \"" + query + "\" and "
                  + "sortOrder " + sortOrder
                  + " (" + slimCollector.getDocumentCount() + " documents) "
                  + " in " + profiler.getSpendTime());
        return structure;

         */
    }
}




