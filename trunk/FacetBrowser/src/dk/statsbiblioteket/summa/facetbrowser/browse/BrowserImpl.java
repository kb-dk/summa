/* $Id: BrowserImpl.java,v 1.14 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.14 $
 * $Date: 2007/10/05 10:20:22 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: BrowserImpl.java,v 1.14 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResult;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetMap;
import dk.statsbiblioteket.summa.facetbrowser.core.FacetCore;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.*;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.rmi.RemoteException;
import java.io.File;
import java.io.IOException;

/**
 * The default browser implementation is synchronized and thus does only
 * support one thread at a time. This is due to caching of tag counters,
 * in order to minimise garbage collections.
 * The implementation uses threading and should scale well on a multi-processor
 * machine, if the architecture supports parallel RAM access. On a machine
 * with multiple processors, but old-style access to RAM, the increase in
 * performance is smaller.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BrowserImpl extends SearchNodeImpl implements Browser {
    private static Logger log = Logger.getLogger(BrowserImpl.class);

    /**
     * Each concurrent search costs total number of tags * 4 bytes of memory.
     */
    public static final int DEFAULT_NUMBER_OF_CONCURRENT_SEARCHES = 1;

    private Structure structure;
    private FacetMap facetMap;

    private BlockingQueue<BrowserThread> browsers;

    public BrowserImpl(Configuration conf) throws RemoteException {
        super(conf);
        log.info("Constructing BrowserImpl");
        structure = new Structure(conf);
        TagHandler tagHandler =
                TagHandlerFactory.getTagHandler(conf, structure, false);
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
        log.trace("BrowserImpl constructed. Awaiting open");
    }

    public void open(File directory) throws IOException {
        super.open(directory.toString());
    }

    protected void managedWarmup(String request) {
        log.trace("managedWarmup(" + request + ") called. No effect for "
                  + "BrowserImpl as it relies on previously chained "
                  + "DocumentSearcher");
    }

    protected void managedClose() throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close() called");
        facetMap.close();
    }

    protected void managedOpen(String location) throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("managedOpen(" + location + ") called");
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

    protected void managedSearch(Request request, ResponseCollection responses)
                                                        throws RemoteException {
        //FacetRequest facetRequest = new FacetRequest()
    }

    public List<String> getFacetNames() {
        return structure.getFacetNames();
    }

    public FacetResult getFacetMap(int[] docIDs, int startPos, int length,
                              String facets) {
        return null;
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
