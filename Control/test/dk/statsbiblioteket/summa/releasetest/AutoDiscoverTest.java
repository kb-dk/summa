/* $Id$
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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Zips;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.api.StorageReaderClient;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.IndexController;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.IndexReader;
import junit.framework.TestCase;

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

    File TEST_DIR = new File(System.getProperty("java.io.tmpdir"), "autotest");
    String STORAGE_CONF_LOCATION = "data/auto/setup/StorageConfiguration.xml";
    String INGEST_FOLDER = new File(TEST_DIR, "data_in").toString();
    String INDEX_ROOT = new File(TEST_DIR, "index").toString();
    File INGEST_SOURCE = new File(Resolver.getURL("data/auto/data").getFile());
    List<String> TEST_FILES = Arrays.asList(
            "gurli.margrethe.xml", "hans.jensen.xml", "jens.hansen.xml");
    String FAGREF_XSLT = Resolver.getURL(
            "data/search/fagref_xslt/fagref_index.xsl").getFile();
    String INDEX_DESCRIPTOR =
            Resolver.getURL("data/auto/setup/IndexDescriptor.xml").getFile();

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

    private StorageService createStorage() throws Exception {
        Configuration conf =
                Configuration.load("data/auto/setup/StorageConfiguration.xml");
        conf.set(DatabaseStorage.CONF_LOCATION, new File(TEST_DIR, "storage"));
        StorageService storage = new StorageService(conf);
        storage.start();
        return storage;
    }
    private ReadableStorage getStorageReader() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(ConnectionConsumer.CONF_RPC_TARGET,
                 "//localhost:28000/summa-storage");
        conf.set(GenericConnectionFactory.CONF_GRACE_TIME, 1);
        conf.set(GenericConnectionFactory.CONF_RETRIES, 1);
        return new StorageReaderClient(conf);
    }
    public void testStorage() throws Exception {
        assertNotNull("The Storage configuration should exist",
                      Resolver.getUTF8Content(STORAGE_CONF_LOCATION));
        StorageService storage = createStorage();
        assertEquals("The Storage should be running",
                     Status.CODE.running, storage.getStatus().getCode());
        ReadableStorage reader = getStorageReader();
        try {
            reader.getModificationTime(null);
        } catch (Exception e) {
            log.fatal("Unable to communicate with Storage", e);
            fail("It should be possible to communicate with the Storage");
        }

        try {
            storage.getStorage().getModificationTime(null);
        } catch (Exception e) {
            log.fatal("Unable to communicate with Storage", e);
            fail("It should be possible to communicate with the Storage");
        }
        storage.stop();
        Thread.sleep(1000);
        assertEquals("The Storage should be stopped",
                     Status.CODE.stopped, storage.getStatus().getCode());
    }

    private FilterService createIngestChain() throws Exception {
        Configuration conf =
                Configuration.load("data/auto/setup/IngestConfiguration.xml");
        conf.set(DatabaseStorage.CONF_LOCATION, new File(TEST_DIR, "storage"));
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
                set(FileReader.CONF_ROOT_FOLDER, INGEST_FOLDER);
/*        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfiguration("FileWatcher").
                set(FileReader.CONF_ROOT_FOLDER, INGEST_FOLDER);*/
        FilterService ingest = new FilterService(conf);
        ingest.start();
        return ingest;
    }
    public void testIngest() throws Exception {
        StorageService storage = createStorage();
        Service ingest = createIngestChain();
        ingest.start();
        putFiles();
        Thread.sleep(2000);
        checkRecords(storage);
        ingest.stop();
        storage.stop();
    }

    private Service createIndexer() throws Exception {
        Configuration conf =
                Configuration.load("data/auto/setup/IndexConfiguration.xml");
        conf.set(DatabaseStorage.CONF_LOCATION, new File(TEST_DIR, "storage"));
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(1).
//                getSubConfiguration("FagrefTransformer").
                set(XMLTransformer.CONF_XSLT, FAGREF_XSLT);
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(3).
//                getSubConfiguration("DocumentCreator").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, INDEX_DESCRIPTOR);
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(4).
                getSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS).get(0).
//                getSubConfiguration("IndexUpdate").
//                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, INDEX_DESCRIPTOR);
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(4).
//                getSubConfiguration("IndexUpdate").
                set(IndexControllerImpl.CONF_INDEX_ROOT_LOCATION, INDEX_ROOT);
        FilterService index = new FilterService(conf);
        index.start();
        return index;
    }
    public void testIndexer() throws Exception {
        StorageService storage = createStorage();
        Service ingest = createIngestChain();

        ingest.start();


        Service index = createIndexer();
        index.start();


        putFiles();

        boolean foundRecs = waitForRecords(storage.getStorage(), 5000,
                       "fagref:gm@example.com", "fagref:hj@example.com",
                       "fagref:jh@example.com");

        assertTrue("The fagref records did not appear in storage withing "
                   + "allowed timeframe", foundRecs);

        assertTrue("The index root must exist", new File(INDEX_ROOT).exists());
        File luceneDir = new File(new File(INDEX_ROOT).listFiles()[0],
                                  "lucene");

        // The records are in storage now, give the indexer 2s to react. This
        // should be sufficient since the default poll time is 500ms
        boolean foundDocs = waitForDocCount(luceneDir, TEST_FILES.size(), 2000);

        assertTrue("The lucene index should eventually contain "
                   + TEST_FILES.size() + " records", foundDocs);

        File facetDir = new File(new File(INDEX_ROOT).listFiles()[0], 
                                 "facet");
        assertTrue("The INDEX_ROOT/YYMMDD_HHMM/facet/author.dat (" + INDEX_ROOT
                   + ") should be > 0 bytes ",
                   facetDir.listFiles()[0].length() > 0);

        ingest.stop();
        index.stop();
        storage.stop();
        log.info("Test succes!");
    }

    private void checkRecords(StorageService storage) throws IOException {
        StorageIterator records = new StorageIterator(
                storage.getStorage(),
                storage.getStorage().getRecordsModifiedAfter(0, "fagref", null));
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
    private boolean waitForRecords(Storage storage, long timeout, String... ids)
                                                            throws Exception {
        boolean foundRecords = false;
        long startTime = System.currentTimeMillis();
        List<String> idList = Arrays.asList(ids);

        while (!foundRecords
               && (System.currentTimeMillis() - startTime < timeout)) {
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

        return foundRecords;
    }

    public boolean waitForDocCount (File index, int docCount, long timeout)
                                                               throws Exception{
        boolean foundDocs = false;
        long startTime = System.currentTimeMillis();

        while (!foundDocs
               && (System.currentTimeMillis() - startTime < timeout)) {
            log.info("Waiting for " + docCount +" documents in: " + index);

            Thread.sleep(100);

            try {
                IndexReader luceneIndex = IndexReader.open(index);
                if (luceneIndex.maxDoc() == docCount) {
                    foundDocs = true;
                } else {
                    log.debug("Index not ready, found " + luceneIndex.maxDoc()
                              + " documents");
                }
                luceneIndex.close();
            } catch (Exception e) {
                // Ignore and rerun loop, the index might not even exist yet!
            }
        }

        if (foundDocs) {
            log.info("Got expected doc count, " + docCount + ", from: "
                     + index);
        } else {
            log.warn("Did NOT get expected docu count from: " + index);
        }

        return foundDocs;
    }

    private SearchService createSearcher() throws Exception {
        Configuration conf =
                Configuration.load("data/auto/setup/SearchConfiguration.xml");
        conf.set(DatabaseStorage.CONF_LOCATION, new File(TEST_DIR, "storage"));
        conf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT, INDEX_ROOT);
        conf.getSubConfigurations(SearchNodeFactory.CONF_NODES).get(0).
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
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
        StorageService storage = createStorage();
        SearchService searchService = createSearcher();
        searchService.start();
        SearchClient searchClient = getSearchClient();
        Service ingest = createIngestChain();
        ingest.start();
        Service index = createIndexer();
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
        storage.stop();
    }
}
