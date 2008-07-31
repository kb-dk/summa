/* $Id: LuceneFacetBrowser.java,v 1.11 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.11 $
 * $Date: 2007/10/05 10:20:24 $
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
 * CVS:  $Id: LuceneFacetBrowser.java,v 1.11 2007/10/05 10:20:24 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser;

import java.io.IOException;

import dk.statsbiblioteket.summa.facetbrowser.browse.Browser;
import dk.statsbiblioteket.summa.facetbrowser.browse.Result;
import dk.statsbiblioteket.summa.facetbrowser.browse.BrowserImpl;
import dk.statsbiblioteket.summa.facetbrowser.core.StructureDescription;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerFactory;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.lucene.index.IndexConnector;
import dk.statsbiblioteket.summa.common.lucene.index.IndexChangeListener;
import dk.statsbiblioteket.summa.common.lucene.index.IndexChangeEvent;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.search.IndexSearcher;
import org.apache.log4j.Logger;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneFacetBrowser implements IndexChangeListener { // implements Browser, Builder, FacetCore {
    private static Logger log = Logger.getLogger(LuceneFacetBrowser.class);

    private Configuration configuration;
    private IndexConnector connector;
    private IndexSearcher searcher;
    private SummaQueryParser queryParser;
    private StructureDescription structure;
    private TagHandler tagHandler;
    private CoreMap coreMap;
    private Browser browser;

    /**
     * Sets up all the necessary elements of the FacetBrowser. Depending on the
     * configuration, this will result in empty facet browser structures,
     * loading of previously saved structures or a complete rebuild of said
     * structures.
     * @param configuration the configuration for the while FacetBrowser.
     * @throws IOException if no connection to the index could be made.
     */
    public LuceneFacetBrowser(Configuration configuration) throws IOException {
        this.configuration = configuration;
        structure = new StructureDescription(configuration);
        connector = new IndexConnector(configuration);
        searcher = connector.getSearcher();
        queryParser = new SummaQueryParser(configuration);
        structure = new StructureDescription(configuration);

        // TODO: Implement logic for reuse, iterated build and clean start
        // TODO: Use IndexDescriptor
        tagHandler =
                TagHandlerFactory.getTagHandler(configuration, null, connector);
//        coreMap = CoreMapFactory.getCoreMap(configuration, connector);
        browser = new BrowserImpl(configuration, searcher, queryParser,
                                  tagHandler, structure, coreMap);
    }

    /* Browser interface */
/*    public String getFacetXML(String queryString, String filterQuery,
                              String queryLang,
                              Result.TagSortOrder sortOrder) {
        return browser.getFacetXML(queryString, filterQuery,
                                   queryLang, sortOrder);
    }
    public Result getFacetMap(String queryString, String filterQuery,
                                      String queryLang,
                                      Result.TagSortOrder sortOrder) {
        return browser.getFacetMap(filterQuery, queryString,
                                   sortOrder, null);
    }*/

    /**
     * Performs a complete rebuild of the mapping from documents to tags.
     * During the rebuild, the FacetBrowser will still be responsive, but it
     * will return result based on its state befor the rebuildMap-call.
     */
    public void rebuildMap() {
        log.info("Rebuilding Facet Browser map");
        log.info("Rebuilding of Facet Browser map finished");
    }

    public void indexChanged(IndexChangeEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Index changed event catched in LuceneFacetBrowser: " + event);
        }
    }
}
