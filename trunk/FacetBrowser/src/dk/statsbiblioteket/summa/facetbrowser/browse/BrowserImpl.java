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
import dk.statsbiblioteket.summa.common.lucene.search.SlimCollector;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;
import org.apache.lucene.search.IndexSearcher;

import java.util.ArrayList;
import java.util.List;

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
public abstract class BrowserImpl implements Browser {
    private static Logger log = Logger.getLogger(BrowserImpl.class);
    public static final String BROWSERTHREADS_PROPERTY =
            "FacetBrowser.browserThreads";
    public static final String BROWSERTHREADS_TIMEOUT_PROPERTY =
            "FacetBrowser.browserThreadTimeout";

    private static final int SINGLE_THREAD_THRESHOLD = 1000;
    private static final int DEFAULT_THREAD_TIMEOUT = 10000; // 10 seconds

    private IndexSearcher searcher;
    private TagHandler tagHandler;
    private Structure structure;
    private CoreMap coreMap;
    private Configuration configuration;
    private SummaQueryParser queryParser;

    private Profiler profiler = new Profiler();

    private int timeout = DEFAULT_THREAD_TIMEOUT;

    private ArrayList<BrowserThread> browsers;
    private SlimCollector slimCollector = new SlimCollector();

    private static final int DEFAULT_BROWSERTHREADS = 2;

    // TODO: Should this use a SummaSearcher?
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public BrowserImpl(Configuration configuration, IndexSearcher searcher,
                       SummaQueryParser queryParser, TagHandler tagHandler,
                       Structure structure, CoreMap coreMap) {
        this.configuration = configuration;
        this.searcher = searcher;
        this.queryParser = queryParser;
        this.tagHandler = tagHandler;
        this.structure = structure;
        this.coreMap = coreMap;
        int browserThreads = DEFAULT_BROWSERTHREADS;
        try {
            browserThreads = configuration.getInt(BROWSERTHREADS_PROPERTY);
        } catch (Exception e) {
            log.warn("Could not get " + BROWSERTHREADS_PROPERTY
                     + " from the configuration. Defaulting to "
                     + DEFAULT_BROWSERTHREADS);
        }
        try {
            timeout = configuration.getInt(BROWSERTHREADS_TIMEOUT_PROPERTY);
        } catch (Exception e) {
            log.warn("Could not get " + BROWSERTHREADS_TIMEOUT_PROPERTY
                     + " from the configuration. Defaulting to "
                     + DEFAULT_THREAD_TIMEOUT);
        }

        browsers = new ArrayList<BrowserThread>(browserThreads);
        for (int i = 0 ; i < browserThreads ; i++) {
            browsers.add(new BrowserThread(tagHandler, coreMap));
        }
    }

/*    public String getFacetXML(String query, String filterQuery,
                              String queryLang,
                              FacetResult.TagSortOrder sortOrder) {
        return getFacetMap(filterQuery, query, sortOrder).toXML();
    }
  */
  /*  public synchronized FacetResult getFacetMap(String filter,
                                                   String query,
                                                   String
                                                           sort, String facets) {
        profiler.reset();
        slimCollector.clean();
        Query query;
        try {
            // TODO: Use queryLang here
            query = queryParser.parse(query);
        } catch (ParseException e) {
            log.warn("Query ParseException with query \"" + query
                     + "\": " + e.getMessage(), e);
            return null;
        }
        Filter filter = null;
        try {
            // TODO: Use queryLang here, cache filters?
            if (filter != null && !"".equals(filter)) {
                filter = new QueryFilter(queryParser.parse(query));
            }
        } catch (ParseException e) {
            log.warn("Query ParseException with filter \"" + filter
                     + "\": " + e.getMessage(), e);
            return null;
        }
        try {
            // TODO: Speed up collection by bypassing score calculation
            if (filter == null) {
                searcher.search(query, slimCollector);
            } else {
                searcher.search(query, filter, slimCollector);
            }
        } catch (IOException e) {
            log.error("IOException perforimg search: " + e.getMessage(), e);
            return null;
        }
        return getFacetMap(query, sort, slimCollector);
    }

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
    }
    */

    public List<String> getFacetNames() {
        return null;
    }

    public FacetResult getFacetMap(int[] docIDs, int startPos, int length,
                              String facets) {
        return null;
    }
}
