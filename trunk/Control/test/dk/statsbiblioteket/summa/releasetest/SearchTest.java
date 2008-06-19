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

import javax.management.NotificationListener;
import javax.management.Notification;
import javax.management.AttributeChangeNotification;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import java.io.File;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.security.Permission;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.service.SearchService;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.search.LuceneSearcher;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.index.IndexController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The purpose of this class is to test the workflow
 * "files => ingest-chain => storage => index-chain => index = search".
 * It relies on the modules Common, Ingest, Index, Storage and Search.
 * </p><p>
 * The method {@link #testFull} is the big test.
 */
// TODO: Fix shutdown problem, so that all unit-tests can run sequentially
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(SearchTest.class);

    public void setUp () throws Exception {
        super.setUp();
        checkSecurityManager();
        cleanup();
        INDEX_ROOT.mkdirs();
    }

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

    private File root = new File(Resolver.getURL(
            "data/search/SearchTest_IngestConfiguration.xml").
            getFile()).getParentFile();
    private String BASE = "fagref";

    private static void checkSecurityManager() {
        if (System.getSecurityManager() == null) {
            log.debug("Setting allow-all security manager");
            System.setSecurityManager(new RMISecurityManager() {
                public void checkPermission(Permission perm) {
                }
                public void checkPermission(Permission perm, Object context) {
                }
            });
        }
    }

    public StorageService startStorage() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        StorageService storage = new StorageService(storageConf);
        storage.start();
        return storage;
    }
        
    /* ingest the data in the given folder to Storage, assuming that Storage
     * is running. */
    private void ingest(File folder) throws Exception {

        Configuration conf = Configuration.load(
                "data/search/SearchTest_IngestConfiguration.xml");
        conf.getSubConfiguration("SingleChain").getSubConfiguration("Reader").
                set(FileReader.CONF_ROOT_FOLDER, folder.getAbsolutePath());

        FilterService ingester = new FilterService(conf);
        ingester.start();
        IndexTest.waitForService(ingester);
        ingester.stop();
    }

    /* Connects to Storage, iterates all records and verifies that the given
       id exists.
     */
    private void verifyStorage(StorageService storage, String id) throws
                                                                     Exception {
        RecordIterator recordIterator =
                storage.getRecordsModifiedAfter(0, BASE);
        assertTrue("The iterator should have at least one element",
                   recordIterator.hasNext());
        while (recordIterator.hasNext()) {
            if (recordIterator.next().getId().equals(id)) {
                return;
            }
        }
        fail("A Record with id '" + id + "' should be present in the storage");
    }

    public void testIngest() throws Exception {
        StorageService storage = startStorage();
        ingest(new File(Resolver.getURL("data/search/input/part1").getFile()));
        verifyStorage(storage, "fagref:hj@example.com");
        ingest(new File(Resolver.getURL("data/search/input/part2").getFile()));
        verifyStorage(storage, "fagref:jh@example.com");
        storage.stop();
    }

    File INDEX_ROOT = new File(System.getProperty("java.io.tmpdir"),
                               "testindex");

    private SearchService createSearchService() throws Exception {
        SearchService searchService =
                new SearchService(getSearcherConfiguration());
        searchService.start();
        return searchService;
    }

    private SummaSearcher createSearcher() throws Exception {
        return new LuceneSearcher(getSearcherConfiguration());
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
            searcher.fullSearch(null, "hans", 0, 10, null, false, null, null);
            fail("An IndexException should be thrown as no index data are"
                 + " present yet");
        } catch (IndexException e) {
            // Expected
        }
    }

    public void testSearchService() throws Exception {
        SearchService searcher = createSearchService();
        try {
            searcher.fullSearch(null, "hans", 0, 10, null, false, null, null);
            fail("An IndexException should be thrown as no index data are"
                 + " present yet");
        } catch (IndexException e) {
            // Expected
        }
        JMXServiceURL url = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://:2783/dk.statsbiblioteket.summa.control.service.SearchService:type=Service");     // 2783
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);

        Thread.sleep(Integer.MAX_VALUE);
        searcher.stop();
    }

    private static class ClientListener implements NotificationListener {
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


    private void updateIndex() throws Exception {
        URL xsltLocation = Resolver.getURL(
                "data/search/fagref_xslt/fagref_index.xsl");
        assertNotNull("The fagref xslt location should not be null",
                      xsltLocation);
        URL descriptorLocation = Resolver.getURL(
                "data/search/SearchTest_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null",
                      descriptorLocation);

        Configuration indexConf = Configuration.load(
                "data/search/SearchTest_IndexConfiguration.xml");
        Configuration chain = indexConf.getSubConfiguration("SingleChain");
        chain.getSubConfiguration("FagrefTransformer").
                set(XMLTransformer.CONF_XSLT, xsltLocation.getFile());
        chain.getSubConfiguration("DocumentCreator").getSubConfiguration(
                LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());
        chain.getSubConfiguration("IndexUpdate").
                set(IndexController.CONF_INDEX_ROOT_LOCATION,
                    INDEX_ROOT.toString());
        chain.getSubConfiguration("IndexUpdate").
                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());

        FilterService indexService = new FilterService(indexConf);
        indexService.start();
        IndexTest.waitForService(indexService);
        indexService.stop();
    }

    private void verifySearch(SummaSearcher searcher, String query,
                              int results) throws Exception {
        log.debug("Verifying existence of " + results
                  + " documents with query '" + query + "'");
        assertEquals("The result for query '" + query
                     + "' should match the expected count",
                     results, getHits(searcher, query));
    }

    private Pattern hitPattern =
            Pattern.compile(".*hitCount\\=\\\"([0-9]+)\\\".*", Pattern.DOTALL);
    private int getHits(SummaSearcher searcher, String query) throws Exception {
        String result = searcher.fullSearch(null, query, 0, 10,
                                            null, false, null, null);
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


    // Set up searcher, check for null
     // Set up storage
     // Run index w/ update on storage
     // Perform small ingest
     // Run index w/ update on storage, verify result in searcher
     // Perform ingest 2
     // Run index w/ update on storage, verify result in searcher
     // Delete record in storage
     // Run index w/ update on storage, verify result in searcher

    public void testFull() throws Exception {
        SummaSearcher searcher = createSearcher();
        try {
            searcher.fullSearch(null, "hans", 0, 10, null, false, null, null);
            fail("An IndexException should be throws as not index data are"
                 + " present yet");
        } catch (IndexException e) {
            // Expected
        }
        StorageService storage = startStorage();
        updateIndex();
        Thread.sleep(2000); // Wait for searcher to discover new content
        assertNotNull("Searching should provide a result (we don't care what)",
                      searcher.fullSearch(null, "dummy", 0, 1, null, false,
                                          null, null));
        ingest(new File(Resolver.getURL("data/search/input/part1").getFile()));
        verifyStorage(storage, "fagref:hj@example.com");
        updateIndex();
        log.debug("Finished updating of index. It should now contain 1 doc");
        Thread.sleep(2000); // Wait for searcher to discover new content
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
        updateIndex();
        log.debug("Finished updating of index. It should now contain 3 docs");
        Thread.sleep(2000); // Wait for searcher to discover new content
        try {
            verifySearch(searcher, "Hans", 1);
        } catch (IndexException e) {
            fail("Failed search 2 for Hans: " + e.getMessage());
        }
        try {
            verifySearch(searcher, "Gurli", 1);
        } catch (IndexException e) {
            fail("Failed search 2 for Gurli: " + e.getMessage());
        }
        log.debug("Calling stop on Storage");
        storage.stop();
    }
}
