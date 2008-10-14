/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.releasetest;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Iterator;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.common.unittest.LuceneTestHelper;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * IMPORTANT: Due to problems with releasing JDBC, the tests cannot be run
 * in succession, but must be started one at a time in their own JVM.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(IndexTest.class);

    public static final String TESTBASE = "fagref";
    public static final int NUM_RECORDS = 3;

    public void setUp () throws Exception {
        super.setUp();
        IngestTest.deleteOldStorages();
        if (INDEX_ROOT.exists()) {
            Files.delete(INDEX_ROOT);
        }
        INDEX_ROOT.mkdirs();
    }

    public void testIngest() throws Exception {
        fillStorage().close();
    }

    public void testNonIterativeIndexing() throws Exception {
        Storage storage = fillStorage();

        assertEquals("There should be no existing indexes", 0, countIndexes());
        Configuration indexConf = Configuration.load(
                "data/search/IndexTest_IndexConfiguration.xml");
        updateIndex(indexConf);
        assertEquals("After update one the number of indexes should be correct",
                     1, countIndexes());
        updateIndex(indexConf);
        assertEquals("After update two the number of indexes should be correct",
                     2, countIndexes());
        storage.close();
    }

    public void testIterativeIndexing() throws Exception {
        Storage storage = fillStorage();

        assertEquals("There should be no existing indexes", 0, countIndexes());
        Configuration indexConf = Configuration.load(
                "data/search/IndexTest_IndexConfiguration.xml");
        indexConf.getSubConfiguration("SingleChain").
                getSubConfiguration("IndexUpdate").
                set(IndexControllerImpl.CONF_CREATE_NEW_INDEX, false);
        updateIndex(indexConf);
        assertEquals("After update one the number of indexes should be correct",
                     1, countIndexes());
        updateIndex(indexConf);
        assertEquals("After update two the number of indexes should be correct",
                     1, countIndexes());
        storage.close();
    }

    private int countIndexes() {
        if (!INDEX_ROOT.exists()) {
            return 0;
        }
        return INDEX_ROOT.listFiles().length;
    }

    // TODO: Implement proper shutdown of single tests

    public static final File INDEX_ROOT = new File(
            System.getProperty("java.io.tmpdir"), "testindex");
    /**
     * Tests the workflow from files on disk to finished index.
     * @throws Exception if the workflow failed.
     */
    public void testWorkflow() throws Exception {
        Storage storage = fillStorage();


        // Index chain setup
        URL xsltLocation =
                Thread.currentThread().getContextClassLoader().getResource(
                        "data/fagref/fagref_index.xsl");
        assertNotNull("The original xslt location should not be null",
                      xsltLocation);
        String descriptorLocation =
                "file://"
                + Thread.currentThread().getContextClassLoader().getResource(
                        "data/fagref/fagref_IndexDescriptor.xml").getFile();
        System.out.println(descriptorLocation);

        String filterConfString =
                Streams.getUTF8Resource("data/fagref/fagref_index_setup.xml");
        // TODO: Update this test to handle the new style with SummaDocument
        filterConfString = filterConfString.replace("/tmp/summatest/data/"
                                                    + "fagref/fagref_index.xsl",
                                                    xsltLocation.toString());
        filterConfString =
                filterConfString.replace(">/tmp/summatest/data/fagref<",
                                         ">" + INDEX_ROOT.toString() + "<");
        filterConfString =
                filterConfString.replace("/tmp/summatest/data/fagref/"
                                         + "fagref_IndexDescriptor.xml",
                                         descriptorLocation);
        // Yes, two replaces
        filterConfString =
                filterConfString.replace("/tmp/summatest/data/fagref/"
                                         + "fagref_IndexDescriptor.xml",
                                         descriptorLocation);

        assertFalse("Replace should work",
                    filterConfString.contains("/tmp/summatest/data/fagref/"
                                              + "fagref_index.xsl"));
        File indexConfFile = new File(System.getProperty("java.io.tmpdir"),
                                                          "indexConf.xml");
        Files.saveString(filterConfString, indexConfFile);

        assertTrue("The index conf. should exist", indexConfFile.exists());
        Configuration filterConf = Configuration.load(indexConfFile.getPath());
        assertNotNull("Configuration should contain "
                      + FilterControl.CONF_CHAINS,
                      filterConf.getString(FilterControl.CONF_CHAINS));

        FilterService indexService = new FilterService(filterConf);
        indexService.start();

        waitForService(indexService);

        List<Filter> filters =
                indexService.getFilterControl().getPumps().get(0).getFilters();
        File indexLocation = ((IndexControllerImpl)filters.get(filters.size()-1)).
                getIndexLocation();

        String[] EXPECTED_IDS = new String[] {"fagref:gm@example.com",
                                              "fagref:hj@example.com",
                                              "fagref:jh@example.com"};
        LuceneTestHelper.verifyContent(
                new File(indexLocation, LuceneIndexUtils.LUCENE_FOLDER),
                EXPECTED_IDS);

        storage.close();
    }

    /**
     * Create a Storage and fill it with test-data, ready for indexing.
     * @return the StorageService containing the filled Storage.
     * @throws Exception if the fill failed.
     */
    public static Storage fillStorage() throws Exception {
        // Storage
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);

        // Ingest
        URL dataLocation =
                Thread.currentThread().getContextClassLoader().getResource(
                        "data/fagref/fagref_testdata.txt");
        assertNotNull("The data location should not be null", dataLocation);
        File ingestRoot = new File(dataLocation.getFile()).getParentFile();
        String filterConfString =
                Streams.getUTF8Resource("data/fagref/fagref_filter_setup.xml");
        filterConfString =
                filterConfString.replace("/tmp/summatest/data/fagref",
                                         ingestRoot.toString());
        assertFalse("Replace should work",
                    filterConfString.contains("/tmp/summatest/data/fagref"));
        File filterConfFile = new File(System.getProperty("java.io.tmpdir"),
                                                          "filterConf.xml");
        Files.saveString(filterConfString, filterConfFile);

        assertTrue("The filter conf. should exist", filterConfFile.exists());
        Configuration filterConf = Configuration.load(filterConfFile.getPath());
        assertNotNull("Configuration should contain "
                      + FilterControl.CONF_CHAINS,
                      filterConf.getString(FilterControl.CONF_CHAINS));

        FilterService ingester = new FilterService(filterConf);
        ingester.start();
        waitForService(ingester);

        long iterKey = storage.getRecordsModifiedAfter(0, TESTBASE);
        Iterator<Record> iterator = new StorageIterator(storage, iterKey);
        assertTrue("The iterator should have at least one element",
                   iterator.hasNext());
        for (int i = 0 ; i < NUM_RECORDS ; i++) {
            assertTrue("Storage should have next for record #" + (i+1),
                       iterator.hasNext());
            Record record = iterator.next();
            assertNotNull("The next should give a record", record);
        }
        assertFalse("After " + NUM_RECORDS + " Records, iterator should finish",
                    iterator.hasNext());

        iterKey = storage.getRecordsModifiedAfter(0, TESTBASE);
        iterator = new StorageIterator(storage, iterKey);
        Record gurli = iterator.next();
        String fileContent =
                Resolver.getUTF8Content("data/fagref/gurli.margrethe.xml");
        assertEquals("The stored content should match the file-content",
                     fileContent.trim(), gurli.getContentAsUTF8().trim());

        return storage;
    }

    public static void waitForService(FilterService service) 
                                  throws RemoteException, InterruptedException {
        int TIMEOUT = 10000;
        long endTime = System.currentTimeMillis() + TIMEOUT;
        while (!service.getStatus().getCode().equals(Status.CODE.stopped) &&
               System.currentTimeMillis() < endTime) {
            log.trace("Sleeping a bit");
            Thread.sleep(100);
        }
        assertTrue("The service '" + service + "' should have stopped by now",
                   service.getStatus().getCode().equals(Status.CODE.stopped));
    }

    public static void updateIndex(Configuration conf) throws Exception {
        URL xsltLocation = Resolver.getURL(
                "data/search/fagref_xslt/fagref_index.xsl");
        assertNotNull("The fagref xslt location should not be null",
                      xsltLocation);
        URL descriptorLocation = Resolver.getURL(
                "data/search/SearchTest_IndexDescriptor.xml");
        assertNotNull("The descriptor location should not be null",
                      descriptorLocation);

        Configuration chain = conf.getSubConfiguration("SingleChain");
        chain.getSubConfiguration("FagrefTransformer").
                set(XMLTransformer.CONF_XSLT, xsltLocation.getFile());
        chain.getSubConfiguration("DocumentCreator").getSubConfiguration(
                LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());
        chain.getSubConfiguration("IndexUpdate").
                set(IndexControllerImpl.CONF_INDEX_ROOT_LOCATION,
                    SearchTest.INDEX_ROOT.toString());
        chain.getSubConfiguration("IndexUpdate").
                getSubConfiguration("LuceneUpdater").
                getSubConfiguration(LuceneIndexUtils.CONF_DESCRIPTOR).
                set(IndexDescriptor.CONF_ABSOLUTE_LOCATION,
                    descriptorLocation.getFile());
        FilterService indexService = new FilterService(conf);
        indexService.start();
        waitForService(indexService);
        indexService.stop();
    }
}



