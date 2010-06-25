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
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * Checks that rebuilding the facets works on an existing index.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RebuildFacetsTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(RebuildFacetsTest.class);

    @Override
    public void setUp () throws Exception {
        super.setUp();
        cleanup();
        SearchTest.INDEX_ROOT.mkdirs();
    }

    @Override
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

    public void testFacetSearch() throws Exception {
        log.debug("Getting configuration for searcher");
        Configuration conf = FacetTest.getSearcherConfiguration();
        log.debug("Creating Searcher");
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        log.debug("Searcher created");
        Storage storage = SearchTest.startStorage();
        log.debug("Ingesting");
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part2").getFile()));
        FacetTest.updateIndex();
        log.debug("Waiting for the searcher to discover the new index");
        searcher.checkIndex(); // Make double sure
        log.debug("Verify index");
        SearchTest.verifySearch(searcher, "Gurli", 1);
        SearchTest.verifySearch(searcher, "Hans", 1);
        FacetTest.verifyFacetResult(searcher, "Gurli");

        log.debug("Opening new searcher");
        searcher.close();
        searcher = new SummaSearcherImpl(conf);
        FacetTest.verifyFacetResult(searcher, "Gurli");

        log.debug("Messing up the facet data");
        searcher.close();
        File[] indexDirs = SearchTest.INDEX_ROOT.listFiles();
        File facetDir = new File(indexDirs[indexDirs.length-1], "facet");
        log.debug("Got facetDir " + facetDir + ". Deleting facet data");
//        File facetFile = new File(facetDir, "author.index");
        File facetFile = new File(facetDir, "coremap.meta");
        assertTrue("The facet-file " + facetFile + " should exist",
                   facetFile.exists());
        facetFile.delete();
        assertFalse("The facet-file " + facetFile + " should be deleted",
                    facetFile.exists());

//        log.debug("Verifying that messing up worked");
//        searcher = new SummaSearcherImpl(conf);
//            searcher.search(SearchTest.simpleRequest("fagekspert"));
//            fail("Searching with corrupt index should give an error");
//        searcher.close();

        log.debug("Repairing facets and verifying that repairing works");
        FacetTest.updateIndex();
        searcher = new SummaSearcherImpl(conf);
        FacetTest.verifyFacetResult(searcher, "Gurli");

        log.debug("Sample output from large search: "
                  + searcher.search(SearchTest.simpleRequest("fagekspert")).
                toXML());
        log.debug("Finished testing, shutting down cleanly");
        storage.close();
    }


}

