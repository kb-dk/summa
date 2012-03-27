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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Sets up an automated workflow where an index is updated automatically when
 * files with new content is dumped in a designated directory. It is meant as
 * a unit-test based test/demonstration of a realistic setup.
 * </p><p>
 * The workflow from a birds eye view:<br />
 * FileWatcher => GUNZIPFilter => XMLSplitterFilter => RecordWriter =>
 * (Storage) => StorageWatcher =>  XMLTransformer <Fagref_XSLT> =>
 * XMLTransformer <OldToNew_XSLT> => DocumentCreator =>
 * IndexController <LuceneManipulator, FacetManipulator> => (Index) =>
 * SummaSearcher <LuceneSearchNode, FacetSearchNode>.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class AutoDiscoverTest extends TestCase {
    private static Log log = LogFactory.getLog(AutoDiscoverTest.class);

    @Override
    public void setUp () throws Exception {
        super.setUp();
        if (TEST_DIR.exists()) {
            try {
                Files.delete(TEST_DIR);
            } catch (Exception e) {
                log.debug("Unable to delete " + TEST_DIR, e);
            }
        }
        TEST_DIR.mkdirs();
        new File(INGEST_FOLDER).mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private File TEST_DIR = new File(System.getProperty("java.io.tmpdir"), "autotest");
    private String INGEST_FOLDER = new File(TEST_DIR, "data_in").toString();
    private File INGEST_SOURCE = Resolver.urlToFile(Resolver.getURL("resources/auto/data"));

    private List<String> TEST_FILES = Arrays.asList(
            "gurli.margrethe.xml", "hans.jensen.xml", "jens.hansen.xml");
    private String FAGREF_XSLT = Resolver.urlToFile(Resolver.getURL(
            "resources/search/fagref_xslt/fagref_index.xsl")).getAbsolutePath();
    private String INDEX_DESCRIPTOR =
            Resolver.getURL("resources/auto/setup/IndexDescriptor.xml").getFile();

    private void putFiles() throws Exception {
        for (String file: TEST_FILES) {
            putFile(file);
//            Thread.sleep(2000); // We need to look into this...
        }
    }
    private void putFile(String file) throws Exception {
        File src = new File(INGEST_SOURCE, file);
        assertTrue("The input file " + src + " should exist", src.exists());
        File dest = new File(INGEST_FOLDER, file + ".gz");
        FileOutputStream out = new FileOutputStream(dest);
        out.write(Zips.gzipBuffer(Files.loadString(src).getBytes("utf-8")));
        out.close();
    }

    /* Creators for the different parts of the workflow. */

    public void testStorage() throws Exception {
        final String STORAGE = "basictest_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);

        StorageReaderClient reader = ReleaseHelper.getReader(STORAGE);
        try {
            reader.getModificationTime(null);
        } catch (Exception e) {
            log.fatal("Unable to communicate with Storage", e);
            fail("It should be possible to communicate with the Storage");
        }
        reader.releaseConnection();
        storage.close();
    }

    private FilterService createIngestChain(String storage) throws Exception {
        Configuration conf = ReleaseHelper.loadGeneralConfiguration(
            storage, "resources/auto/setup/IngestConfiguration.xml");
        conf.set(DatabaseStorage.CONF_LOCATION, new File(TEST_DIR, "storage"));
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
                set(FileReader.CONF_ROOT_FOLDER, INGEST_FOLDER);
/*        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfiguration("FileWatcher").
                set(FileReader.CONF_ROOT_FOLDER, INGEST_FOLDER);*/
        return new FilterService(conf);
    }
    public void testIngest() throws Exception {
        final String STORAGE = "ingest_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        Service ingest = createIngestChain(STORAGE);
        ingest.start();
        putFiles();
        Thread.sleep(2000);
        checkRecords(STORAGE);
        ingest.stop();
        storage.close();
    }

    private Service createIndexer(String storage) throws Exception {
        Configuration conf = IndexTest.loadFagrefProperties(storage, "resources/auto/setup/IndexConfiguration.xml");
        // Special IndexDescriptor (do we really need it or can we just use fagref descriptor?)
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(3).
//                getSubConfiguration("DocumentCreator").
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, INDEX_DESCRIPTOR);
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(4).
                getSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS).get(0).
//                getSubConfiguration("IndexUpdate").
//                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, INDEX_DESCRIPTOR);
        return new FilterService(conf);
    }
    public void testIndexer() throws Exception {
        final String STORAGE = "index_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        Service ingest = createIngestChain(STORAGE);
        ingest.start();

        Service index = createIndexer(STORAGE);
        index.start();

        putFiles();

        boolean foundRecs = waitForRecords(
            STORAGE, 5000, "fagref:gm@example.com", "fagref:hj@example.com", "fagref:jh@example.com");

        assertTrue("The fagref records did not appear in storage withing allowed timeframe", foundRecs);

        String INDEX_ROOT = ReleaseHelper.INDEX_ROOT;
        assertTrue("The index root must exist", new File(INDEX_ROOT).exists());
        File luceneDir = new File(new File(INDEX_ROOT).listFiles()[0],
                                  "lucene");

        // The records are in storage now, give the indexer 2s to react. This
        // should be sufficient since the default poll time is 500ms
        int foundDocs = waitForDocCount(luceneDir, TEST_FILES.size(), 4000);

        assertEquals("The lucene index should eventually contain " + TEST_FILES.size() + " records",
                     TEST_FILES.size(), foundDocs);

        File facetDir = new File(new File(INDEX_ROOT).listFiles()[0], "facet");
        assertTrue("The INDEX_ROOT/YYMMDD_HHMM/facet/author.dat (" + INDEX_ROOT + ") should be > 0 bytes ",
                   facetDir.listFiles()[0].length() > 0);

        ingest.stop();
        index.stop();
        Thread.sleep(1000); // Give them a moment to avoid exceptions
        storage.close();
        log.info("Test succes!");
    }

    private void checkRecords(String storage) throws IOException {
        StorageReaderClient reader = ReleaseHelper.getReader(storage);
        StorageIterator records = new StorageIterator(reader, reader.getRecordsModifiedAfter(0, "fagref", null));
        int count = 0;
        while (records.hasNext()) {
            records.next();
            count++;
        }
        assertEquals("The number of ingested records should match the files",
                     TEST_FILES.size(), count);
    }

    /*
     * Block up to timeout milliseconds for a given set of records.
     * Returns true if the records are found withing the allowed time,
     * false otherwise
     */
    private boolean waitForRecords(String storageID, long timeout, String... ids) throws Exception {
        StorageReaderClient storage = new StorageReaderClient(ReleaseHelper.getStorageClientConfiguration(storageID));
        boolean foundRecords = false;
        long startTime = System.currentTimeMillis();
        List<String> idList = Arrays.asList(ids);

        while (!foundRecords && (System.currentTimeMillis() - startTime < timeout)) {
            log.info("Waiting for records: " + Strings.join(ids, ", "));
            Thread.sleep(100);

            List<Record> recs = storage.getRecords(idList, null);

            // Sanity check
            if (recs.size() != ids.length) {
                continue;
            }

            // Make sure the ids of the results match our request
            for (Record r : recs) {
                foundRecords = true;
                if (!idList.contains(r.getId())) {
                    foundRecords = false;
                    break;
                }
            }
        }

        if (foundRecords) {
            log.info("Found records: " + Strings.join(ids, ", "));
        } else {
            log.warn("Did NOT find records: " + Strings.join(ids, ", "));
        }
        storage.releaseConnection();
        return foundRecords;
    }

    public int waitForDocCount (File index, int docCount, long timeout)
                                                               throws Exception{
        boolean foundDocs = false;
        long startTime = System.currentTimeMillis();
        int maxCount = 0;

        while (!foundDocs && (System.currentTimeMillis() - startTime < timeout)) {
            log.debug("Waiting for " + docCount + " documents in: " + index);

            Thread.sleep(200);

            if (!index.exists()) {
                log.debug("No index yet in '" + index + "'");
            }
            try {
                IndexReader luceneIndex = IndexReader.open(new NIOFSDirectory(index));
                maxCount = luceneIndex.maxDoc();
                if (maxCount == docCount) {
                    foundDocs = true;
                } else {
                    log.debug("Index not ready, found " + luceneIndex.maxDoc() + " documents");
                }
                luceneIndex.close();
            } catch (Exception e) {
                log.debug("Skipping Exception encountered while waiting for index", e);
            }
        }

        if (foundDocs) {
            log.info("Got expected doc count, " + docCount + ", from: " + index);
        } else {
            if (index.exists()) {
                log.warn("Did NOT get expected docu count from: " + index + " with " + index.listFiles().length
                         + " files");
            } else {
                log.warn("Did NOT get expected docu count as there are no index at " + index);
            }
        }

        return maxCount;
    }

    private SearchService createSearcher() throws Exception {
        Configuration conf = IndexTest.loadFagrefProperties("not_used", "resources/auto/setup/SearchConfiguration.xml");
        conf.getSubConfigurations(SearchNodeFactory.CONF_NODES).get(0).
                getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, INDEX_DESCRIPTOR);
        return new SearchService(conf);
    }
    private SearchClient getSearchClient() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(ConnectionConsumer.CONF_RPC_TARGET,
                 "//localhost:28000/summa-searcher");
        return new SearchClient(conf);
    }
    public void testSearch() throws Exception {
        final String STORAGE = "index_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        SearchService searchService = createSearcher();
        searchService.start();
        SearchClient searchClient = getSearchClient();
        Service ingest = createIngestChain(STORAGE);
        ingest.start();
        Service index = createIndexer(STORAGE);
        index.start();
        putFiles();
        Thread.sleep(5000);

        Request request = new Request();
        request.put(DocumentSearcher.SEARCH_QUERY, "hans");

        String response = searchClient.search(request).toXML();
        log.debug("A search for 'hans' gave\n" + response);
        assertTrue("The search for hans should yield a response with "
                   + "Hans-related content", 
                   response.indexOf("Fagekspert i Datalogi") > 0);
        searchService.stop();
        ingest.stop();
        index.stop();
        storage.close();
    }
}

