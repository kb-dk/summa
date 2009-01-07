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
import java.rmi.RemoteException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Iterator;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.util.Security;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.search.*;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    @Override
    public void setUp () throws Exception {
        super.setUp();
        Security.checkSecurityManager();
        cleanup();
        INDEX_ROOT.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        cleanup();
    }

    private void cleanup() throws Exception {
        IngestTest.deleteOldStorages();
        if (INDEX_ROOT.exists()) {
            Files.delete(INDEX_ROOT);
        }
    }

    public static File root = new File(Resolver.getURL(
            "data/search/SearchTest_IngestConfiguration.xml").
            getFile()).getParentFile();
    public static String BASE = "fagref";

    public static Storage startStorage() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        storageConf.set(DatabaseStorage.CONF_CREATENEW, true);
        storageConf.set(DatabaseStorage.CONF_FORCENEW, true);
        return StorageFactory.createStorage(storageConf);
    }
        
    /* ingest the data in the given folder to Storage, assuming that Storage
     * is running. */
    public static void ingest(File folder) throws Exception {

        Configuration conf = Configuration.load(
                "data/search/SearchTest_IngestConfiguration.xml");
        conf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
//                getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER, folder.getAbsolutePath());

        FilterService ingester = new FilterService(conf);
        ingester.start();
        IndexTest.waitForService(ingester);
        ingester.stop();
    }

    /* Connects to Storage, iterates all records and verifies that the given
       id exists.
     */
    public static void verifyStorage(Storage storage, String id) throws
                                                                     Exception {
        long iterKey = storage.getRecordsModifiedAfter(0, BASE, null);
        Iterator<Record> recordIterator = new StorageIterator(storage, iterKey);

        assertTrue("The iterator should have at least one element",
                   recordIterator.hasNext());
        while (recordIterator.hasNext()) {
            if (recordIterator.next().getId().equals(id)) {
                while (recordIterator.hasNext()) {
                    if (recordIterator.next().getId().equals(id)) {
                        fail("More than ine Record with id '" + id + " was "
                             + "present in the Storage");
                    }
                }
                return;
            }
        }
        fail("A Record with id '" + id + "' should be present in the storage");
    }

    public void testIngest() throws Exception {
        Storage storage = startStorage();
        ingest(new File(Resolver.getURL("data/search/input/part1").getFile()));
        verifyStorage(storage, "fagref:hj@example.com");
        ingest(new File(Resolver.getURL("data/search/input/part2").getFile()));
        verifyStorage(storage, "fagref:jh@example.com");
        storage.close();
    }

    public static File INDEX_ROOT =
            new File(System.getProperty("java.io.tmpdir"), "testindex");

    private SummaSearcher createSearchService() throws Exception {
        return SummaSearcherFactory.createSearcher(getSearcherConfiguration());
    }

    private SummaSearcher createSearcher() throws Exception {
        return new SummaSearcherImpl(getSearcherConfiguration());
    }

    private Configuration getSearcherConfiguration() throws IOException {
        URL descriptorLocation = Resolver.getURL(
                "data/search/SearchTest_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null",
                      descriptorLocation);

        Configuration searcherConf = Configuration.load(
                "data/search/SearchTest_SearchConfiguration.xml");
        assertNotNull("The configuration should not be empty",
                      searcherConf);
        searcherConf.getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
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
            fail("A RemoteException should be thrown after timeout as no index " 
                 + "data are present yet");
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
    public static void updateIndex() throws Exception {
        Configuration indexConf = Configuration.load(
                "data/search/SearchTest_IndexConfiguration.xml");
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
    public static int getHits(SummaSearcher searcher, String query) throws
                                                                    Exception {
        String result = searcher.search(simpleRequest(query)).toXML();
        log.debug("Result from search for '" + query + "': " + result);
        Matcher matcher = hitPattern.matcher(result);
        if (!matcher.matches()) {
            throw new NullPointerException("Could not locate hitcount in " 
                                           + result);
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


    public void testFullSearcher() throws Exception {
        testFullSearcher(createSearcher());
        log.debug("Calling close on Storage");
    }

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
    public static void testFullSearcher(SummaSearcher searcher) throws
                                                                Exception {
        log.debug("testFullSearcher started. Performing search");
        try {
            searcher.search(simpleRequest("hans"));
            fail("A RemoteException should be thrown after timeout as no index"
                 + " data are present yet");
        } catch (RemoteException e) {
            // Expected
        }
        Storage storage = startStorage();
        log.debug("Storage started");
        updateIndex();
        log.debug("updateIndex called");
//        Thread.sleep(3000); // Wait for searcher to discover new content
        try {
            searcher.search(simpleRequest("dummy"));
            fail("A search with empty index should fail");
        } catch (Exception e) {
            // Expected
        }
        ingest(new File(Resolver.getURL("data/search/input/part1").getFile()));
        verifyStorage(storage, "fagref:hj@example.com");
        updateIndex();
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
        ingest(new File(Resolver.getURL("data/search/input/part2").getFile()));
        verifyStorage(storage, "fagref:hj@example.com");
        updateIndex();
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



