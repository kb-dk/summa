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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.storage.api.RecordIterator;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Tests a ingest => storage => index => search chain with facets.
 * </p><p>
 * IMPORTANT: Due to problems with releasing JDBC, the tests cannot be run
 * in succession, but must be started one at a time in their own JVM.
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
//        cleanup();
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
        List<Configuration> subSearcherConfs =
                searcherConf.getSubConfigurations(SearchNodeFactory.CONF_NODES);
        for (Configuration subSearcherConf: subSearcherConfs) {
            if (subSearcherConf.getString(
                    SearchNodeFactory.CONF_NODE_CLASS).equals(
                    LuceneSearchNode.class.getName())) {
                Configuration descConf = subSearcherConf.getSubConfiguration(
                        LuceneIndexUtils.CONF_DESCRIPTOR);
                log.debug("Updating the location of the IndexDescriptor to "
                          + descriptorLocation.getFile());
                assertTrue("The descriptorlocation should be present",
                           descConf.valueExists(
                                   IndexDescriptor.CONF_ABSOLUTE_LOCATION));
                descConf.set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                             descriptorLocation.getFile());
            }
        }
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT,
                         SearchTest.INDEX_ROOT.toString());
        return searcherConf;
    }

/* Does not work as the updateIndex-method for SerachTest does not handle facets
   public void testPlainSearch() throws Exception {
        SummaSearcher searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        SearchTest.testFullSearcher(searcher);
    }*/

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
        SearchTest.verifySearch(searcher, "Hans", 1);
        log.debug("Second test-search performed with success");
        verifyFacetResult(searcher, "Hans");
        log.debug("All OK. Closing searcher, storage and returning");
        searcher.close();
        storage.close();
    }

    private void verifyFacetResult(SummaSearcher searcher,
                                   String query) throws IOException {
        String res = searcher.search(SearchTest.simpleRequest(query)).toXML();
        if (!res.contains("<facet name=\"author\">")) {
            fail("Search for '" + query
                 + "' did not produce any facets. Result was:\n" + res);
        }
    }

    public void testTwoDocumentsOneHitSearch() throws Exception {
        Storage storage = SearchTest.startStorage();
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part2").getFile()));
        updateIndex();
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        log.debug("Searcher created. Verifying existence of Gurli data");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        log.debug("Sample output from search: "
                  + searcher.search(SearchTest.simpleRequest("Gurli")).toXML());
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Jens", 1);
        log.debug("Second test-search performed with success");
        verifyFacetResult(searcher, "Jens");
        verifyFacetResult(searcher, "Gurli");
        log.debug("All OK. Closing searcher, storage and returning");
        searcher.close();
        storage.close();
    }

    public void testThreeFileIngest() throws Exception {
        Storage storage = SearchTest.startStorage();
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1plus2").getFile()));
        assertEquals("The Records-count should be correct after first ingest",
                     3, countRecords(storage, "fagref"));

        updateIndex();
        Thread.sleep(5000); // Why do we need to do this?
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        Thread.sleep(5000); // Why do we need to do this?
        searcher.checkIndex(); // Make double sure
        log.debug("Searcher created");
        for (String name: "Jens Gurli Hans".split(" ")) {
            log.debug(String.format("Verifying existence of %s data", name));
            SearchTest.verifySearch(searcher, name, 1);
            verifyFacetResult(searcher, name);
        }
        searcher.close();
        storage.close();
    }

    public void testTagCounting() throws Exception {
        Storage storage = SearchTest.startStorage();
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/partClone").getFile()));
        assertEquals("The Records-count should be correct after first ingest",
                     5, countRecords(storage, "fagref"));

        updateIndex();
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        searcher.checkIndex(); // Make double sure
        log.debug("Searcher created");
        for (String name: "Jens Gurli Hans".split(" ")) {
            log.debug(String.format("Verifying existence of %s data", name));
            SearchTest.verifySearch(
                    searcher, name, name.equals("Hans") ? 3 : 1);
            verifyFacetResult(searcher, name);
        }
        log.debug("Result for search for fagref "
                  + searcher.search(SearchTest.simpleRequest(
                "fagekspert")).toXML());
        searcher.close();
        storage.close();
    }

    public void testDualIngest() throws Exception {
        Storage storage = SearchTest.startStorage();
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        assertEquals("The Records-count should be correct after first ingest",
                     1, countRecords(storage, "fagref"));
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part2").getFile()));
        assertEquals("The Records-count should be correct after second ingest",
                     3, countRecords(storage, "fagref"));

        updateIndex();
        Thread.sleep(5000); // Why do we need to do this?
        log.debug("Index updated. Creating searcher");
        SummaSearcherImpl searcher =
                new SummaSearcherImpl(getSearcherConfiguration());
        Thread.sleep(5000); // Why do we need to do this?
        searcher.checkIndex(); // Make double sure
        log.debug("Searcher created");
        for (String name: "Jens Gurli Hans".split(" ")) {
            log.debug(String.format("Verifying existence of %s data", name));
            SearchTest.verifySearch(searcher, name, 1);
            verifyFacetResult(searcher, name);
        }
        searcher.close();
        storage.close();
    }

    private int countRecords(Storage storage, String base) throws IOException {
        RecordIterator i = storage.getRecords(base);
        int counter = 0;
        while (i.hasNext()) {
            counter++;
            i.next();
        }
        return counter;
    }

    public void testFacetSearch() throws Exception {
        log.debug("Getting configuration for searcher");
        Configuration conf = getSearcherConfiguration();
        log.debug("Creating Searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        log.debug("Searcher created");
        Storage storage = SearchTest.startStorage();
        log.debug("Storage started");
        updateIndex();
        log.debug("Update 1 performed");
        searcher.checkIndex();
        log.debug("CheckIndex called");
        try {
            searcher.search(SearchTest.simpleRequest("dummy"));
            // TODO: Check if this is what we want
            fail("An timeout-Exceptions hould be thrown with empty index");
        } catch (RemoteException e) {
            // Expected
        }
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        log.debug("Ingest 1 performed");
        updateIndex();
        log.debug("Update 2 performed");
        searcher.checkIndex();
        log.debug("Checkindex after Update 2 performed, verifying...");
        SearchTest.verifySearch(searcher, "Hans", 1);
        verifyFacetResult(searcher, "Hans");
        log.debug("Sample output after initial ingest: "
                  + searcher.search(SearchTest.simpleRequest("fagekspert")).
                toXML());
        log.debug("Adding new material");
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part2").getFile()));
        updateIndex();
        log.debug("Waiting for the searcher to discover the new index");
        searcher.checkIndex(); // Make double sure
        log.debug("Verify final index");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Gurli", 1); // Yes, we try again
        SearchTest.verifySearch(searcher, "Hans", 1);
        verifyFacetResult(searcher, "Gurli");
        verifyFacetResult(searcher, "Hans"); // Why is Hans especially a problem?
        log.debug("Sample output from large search: "
                  + searcher.search(SearchTest.simpleRequest("fagekspert")).
                toXML());

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
        IndexTest.updateIndex(indexConf);
    }
}



