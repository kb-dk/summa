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
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.Security;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.SummaSearcherFactory;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.filter.RecordWriter;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The purpose of this class is to test the workflow
 * "files => ingest-chain => storage => index-chain => index = search".
 * It relies on the modules Common, Ingest, Index, Storage and Search.
 * </p><p>
 * IMPORTANT: Due to problems with releasing JDBC, the tests cannot be run
 * in succession, but must be started one at a time in their own JVM.
 */
// TODO: Fix shutdown problem, so that all unit-tests can run sequentially
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(SearchTest.class);

    public final static String fagref_hj =
        Resolver.getFile("search/input/part1").getAbsolutePath();
    public final static String fagref_jh_gm =
        Resolver.getFile("search/input/part2").getAbsolutePath();
    public final static String fagref_1plus2 =
        Resolver.getFile("search/input/part1plus2").getAbsolutePath();
    public final static String fagref_clone =
        Resolver.getFile("search/input/partClone").getAbsolutePath();

    @Override
    public void setUp () throws Exception {
        super.setUp();
        Security.checkSecurityManager();
        cleanup();
        if (!INDEX_ROOT.mkdirs()) {
            throw new IOException("Unable to create folder " + INDEX_ROOT);
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        cleanup();
    }

    private void cleanup() throws Exception {
        ReleaseHelper.cleanup();
        if (INDEX_ROOT.exists()) {
            Files.delete(INDEX_ROOT);
        }
        ReleaseHelper.cleanup();
    }

    public static File root = new File(Resolver.getURL(
            "search/SearchTest_IngestConfiguration.xml").
            getFile()).getParentFile();
    public static String BASE = "fagref";

    public void testResourceCopying() {
        assertTrue("Source '" + fagref_hj + "' should exist",
                   Resolver.getFile(fagref_hj).exists());
    }

    /* ingest the data in the given folder to Storage, assuming that Storage
     * is running. */
/*    public static void ingest(String storage, File folder) throws Exception {
        Configuration conf = Configuration.load(
                "search/SearchTest_IngestConfiguration.xml");
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
//                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER, folder.getAbsolutePath());

        FilterService ingester = new FilterService(conf);
        ingester.start();
        IndexTest.waitForService(ingester);
        ingester.stop();
    }*/

    public static void update(Record record) throws Exception {
        Payload payload = new Payload(record);
        PayloadFeederHelper feeder =
            new PayloadFeederHelper(Arrays.asList(payload));
                                                        
        Configuration conf = Configuration.load("search/SearchTest_IngestConfiguration.xml");
        Configuration writerConf =
            conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(2);
//                getSubConfiguration("Writer").
        RecordWriter writer = new RecordWriter(writerConf);

        writer.setSource(feeder);
        writer.pump();
        writer.close(true);
    }

    public void testIngest() throws Exception {
        final String STORAGE = "search_ingest_storage";
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        ingestFagref(STORAGE, fagref_hj);
        verifyStorage(STORAGE, "hj", "fagref:hj@example.com");
        ingestFagref(STORAGE, fagref_jh_gm);
        verifyStorage(STORAGE, "jh", "fagref:jh@example.com");
        storage.close();
    }

    private static void verifyStorage(
        String storage, String feedback, String recordID) throws IOException {
        assertNotNull("A Record for " + feedback + " should be present",
                      ReleaseHelper.getRecord(storage, recordID));
    }

    public static void ingestFagref(String storage, String source) {
        ReleaseHelper.ingest(
            storage, source, "fagref", "fagref:", "fagref", "email");
    }

    public final static File INDEX_ROOT =
            new File(System.getProperty("java.io.tmpdir"), "testindex");

    private SummaSearcher createSearchService() throws Exception {
        return SummaSearcherFactory.createSearcher(getSearcherConfiguration());
    }

    private SummaSearcher createSearcher() throws Exception {
        return new SummaSearcherImpl(getSearcherConfiguration());
    }

    private Configuration getSearcherConfiguration() throws Exception {
        URL descriptorLocation = Resolver.getURL("search/SearchTest_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null", descriptorLocation);

        Configuration searcherConf = Configuration.load("search/SearchTest_SearchConfiguration.xml");
        assertNotNull("The configuration should not be empty",
                      searcherConf);
        searcherConf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());
        searcherConf.set(IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT,
                         INDEX_ROOT.toString());
        return searcherConf;
    }

    public void testSearcher() throws Exception {
        SummaSearcher searcher = createSearcher();
        try {
            searcher.search(simpleRequest("hans"));
            fail("A RemoteException should be thrown after timeout as no index data are present yet");
        } catch (RemoteException e) {
            // Expected
        }
    }

    public static Request simpleRequest(String query) {
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        return request;
    }

    public void NonfunctioningYettestSearchService() throws Exception {
        SummaSearcher searcher = createSearchService();
        try {
            searcher.search(simpleRequest("hans"));
            fail("An IndexException should be thrown as no index data are"
                 + " present yet");
        } catch (IndexException e) {
            // Expected
        }
/*        JMXServiceURL url = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://:2783/dk.statsbiblioteket.summa."
                + "control.service.SearchService:type=Service");     // 2783
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
*/
        Thread.sleep(Integer.MAX_VALUE);
    }

/*    private static class ClientListener implements NotificationListener {
            public void handleNotification(Notification notification,
                                           Object handback) {
                log.debug("\nReceived notification:");
                log.debug("\tClassName: " + notification.getClass().getName());
                log.debug("\tSource: " + notification.getSource());
                log.debug("\tType: " + notification.getType());
                log.debug("\tMessage: " + notification.getMessage());
                if (notification instanceof AttributeChangeNotification) {
                    AttributeChangeNotification acn =
                        (AttributeChangeNotification) notification;
                    log.debug("\tAttributeName: " + acn.getAttributeName());
                    log.debug("\tAttributeType: " + acn.getAttributeType());
                    log.debug("\tNewValue: " + acn.getNewValue());
                    log.debug("\tOldValue: " + acn.getOldValue());
                }
            }
        }

  */
    public static void updateIndex(String storageID) throws Exception {
        Configuration indexConf = IndexTest.loadFagrefProperties(
            storageID, "search/SearchTest_IndexConfiguration.xml");
        IndexTest.updateIndex(indexConf);
    }

    public static void verifySearch(SummaSearcher searcher, String query,
                              int results) throws Exception {
        log.debug("Verifying existence of " + results
                  + " documents with query '" + query + "'");
        assertEquals("The result for query '" + query
                     + "' should match the expected count",
                     results, getHits(searcher, query));
    }

    private static Pattern hitPattern =
            Pattern.compile(".*hitCount\\=\\\"([0-9]+)\\\".*", Pattern.DOTALL);
    public static int getHits(SummaSearcher searcher, String query) throws Exception {
        String result = searcher.search(simpleRequest(query)).toXML();
        log.debug("Result from search for '" + query + "': " + result);
        Matcher matcher = hitPattern.matcher(result);
        if (!matcher.matches()) {
            throw new NullPointerException("Could not locate hitcount in " + result);
        }
        return Integer.parseInt(matcher.group(1));
    }

    public static int getHits(Response response) throws Exception {
        String result = response.toXML();
        Matcher matcher = hitPattern.matcher(result);
        if (!matcher.matches()) {
            throw new NullPointerException("Could not locate hitcount in " + result);
        }
        return Integer.parseInt(matcher.group(1));
    }

    public void testHitPattern() throws Exception {
        String TEST = " searchTime=\"42\" hitCount=\"3\">\n";
        Matcher matcher = hitPattern.matcher(TEST);
        assertTrue("hitPattern should match",
                   matcher.matches());
        assertEquals("hitPattern should match 3",
                     "3", matcher.group(1));
    }


/*
    We need to create an index here!
    
    public void testFullSearcher() throws Exception {
        testFullSearcher(createSearcher());
        log.debug("Calling close on Storage");
    }*/

    // Set up searcher, check for null
     // Set up storage
     // Exec index w/ update on storage
     // Perform small ingest
     // Exec index w/ update on storage, verify result in searcher
     // Perform ingest 2
     // Exec index w/ update on storage, verify result in searcher
     // Delete record in storage
     // Exec index w/ update on storage, verify result in searcher

    // TODO: The test fails sometimes, probably a race-condition. Fix it!
    public static void testFullSearcher(SummaSearcher searcher) throws Exception {
        final String STORAGE = "search_full_storage";

        log.debug("testFullSearcher started. Performing search");
        try {
            searcher.search(simpleRequest("hans"));
            fail("A RemoteException should be thrown after timeout as no index data are present yet");
        } catch (RemoteException e) {
            // Expected
        }
        Storage storage = ReleaseHelper.startStorage(STORAGE);
        log.debug("Storage started");
        updateIndex(STORAGE);
        log.debug("updateIndex called");
//        Thread.sleep(3000); // Wait for searcher to discover new content
        try {
            searcher.search(simpleRequest("dummy"));
            fail("A search with empty index should fail");
        } catch (Exception e) {
            // Expected
        }
        ingestFagref(STORAGE, Resolver.getURL(fagref_hj).getFile());
        verifyStorage(STORAGE, "hj", "fagref:hj@example.com");
        updateIndex(STORAGE);
        log.debug("Finished updating of index. It should now contain 1 doc");
        Thread.sleep(5000); // Wait for searcher to discover new content
        try {
            verifySearch(searcher, "Hans", 1);
        } catch (IndexException e) {
            fail("Failed search 1 for Hans: " + e.getMessage());
        }
        try {
            verifySearch(searcher, "Gurli", 0);
        } catch (IndexException e) {
            fail("Failed search 1 for Gurli: " + e.getMessage());
        }
        log.debug("Adding new material");
        ingestFagref(STORAGE, fagref_jh_gm);
        verifyStorage(STORAGE, "hj", "fagref:hj@example.com");
        updateIndex(STORAGE);
        log.debug("Finished updating of index. It should now contain 3 docs");
        Thread.sleep(5000); // Wait for searcher to discover new content
        try {
            verifySearch(searcher, "Gurli", 1);
        } catch (IndexException e) {
            fail("Failed search 2 for Gurli: " + e.getMessage());
        }
        try {
            verifySearch(searcher, "Hans", 1);
        } catch (IndexException e) {
            fail("Failed search 2 for Hans: " + e.getMessage());
        }
        storage.close();
    }

}

