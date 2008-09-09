/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.releasetest;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.storage.api.Storage;

/**
 * Tests a ingest => storage => index => search chain with facets.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(FacetTest.class);

    public void setUp () throws Exception {
        super.setUp();
        cleanup();
        SearchTest.INDEX_ROOT.mkdirs();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        cleanup();
    }

    private void cleanup() throws Exception {
        IngestTest.deleteOldStorages();
        if (SearchTest.INDEX_ROOT.exists()) {
            Files.delete(SearchTest.INDEX_ROOT);
        }
    }

    private Configuration getSearcherConfiguration() throws IOException {
        URL descriptorLocation = Resolver.getURL(
                "data/search/SearchTest_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null",
                      descriptorLocation);

        Configuration searcherConf = Configuration.load(
                "data/search/FacetTest_SearchConfiguration.xml");
        assertNotNull("The Facet configuration should not be empty",
                      searcherConf);
        searcherConf.getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT,
                         SearchTest.INDEX_ROOT.toString());
        return searcherConf;
    }

    public void testPlainSearch() throws Exception {
        SummaSearcher searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        SearchTest.testFullSearcher(searcher);
    }

    public void testCreateSearcher() throws Exception {
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created: " + searcher);
        searcher.close();
    }

    public void testUpdateBlankIndex() throws Exception {
        Storage storage = SearchTest.startStorage();
        log.debug("Storage started");
        updateIndex();
        storage.close();
    }

    public void testIngest() throws Exception {
        Storage storage = SearchTest.startStorage();
        log.debug("Storage started");
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        assertNotNull("Hans Jensen data should be ingested",
                      storage.getRecord("fagref:hj@example.com"));
        storage.close();
    }

    public void testSimpleSearch() throws Exception {
        Storage storage = SearchTest.startStorage();
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        updateIndex();
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created. Verifying existence of Hans Jensen data");
        SearchTest.verifySearch(searcher, "Hans", 1);
        log.debug("Sample output from search: "
                  + searcher.search(SearchTest.simpleRequest("Hans")).toXML());
        log.debug("All OK. Closing searcher, storage and returning");
        searcher.close();
        storage.close();
    }

    public void testFacetSearch() throws Exception {
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created");
        Storage storage = SearchTest.startStorage();
        log.debug("Storage started");
        updateIndex();
        log.debug("Update 1 performed");
        searcher.checkIndex();
        log.debug("CheckIndex called");
        assertNotNull("Searching should provide a result (we don't care what)",
                      searcher.search(SearchTest.simpleRequest("dummy")));
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        log.debug("Ingest 1 performed");
        updateIndex();
        log.debug("Update 2 performed");
        searcher.checkIndex();
        SearchTest.verifySearch(searcher, "Hans", 1);
        log.debug("Adding new material");
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part2").getFile()));
        updateIndex();
        searcher.checkIndex();
        Thread.sleep(3000); // Why do we need to do this?
        SearchTest.verifySearch(searcher, "Hans", 1);
        SearchTest.verifySearch(searcher, "Gurli", 1);
        log.debug("Sample output from search: "
                  + searcher.search(SearchTest.simpleRequest("Hans")).toXML());

        searcher.close();
        storage.close();
    }

    public void testFacetBuild() throws Exception {
        Storage storage = SearchTest.startStorage();
        updateIndex();
        storage.close();
    }

    private void updateIndex() throws Exception {
        Configuration indexConf = Configuration.load(
                "data/search/FacetTest_IndexConfiguration.xml");
        SearchTest.updateIndex(indexConf);
    }
}
