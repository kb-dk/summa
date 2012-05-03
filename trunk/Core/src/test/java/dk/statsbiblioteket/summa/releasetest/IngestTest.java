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

import java.io.File;
import java.util.Iterator;
import java.util.List;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.filter.object.FilterSequence;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.ingest.stream.ArchiveReader;
import dk.statsbiblioteket.summa.ingest.split.XMLSplitterFilter;
import dk.statsbiblioteket.summa.storage.api.*;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The purpose of this class is to test "files => ingest-chain => storage".
 * It relies on the modules Common, Ingest and Storage.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IngestTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(IngestTest.class);

    private final static String HEADER =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    + "<outer xmlns=\"http://www.example.com/mynamespace/\""
    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
    + " xmlns:foo=\"http://example.com/somename\">\n"
    + "<sometag>Hello World</sometag>\n"
    + "<innerrecords>\n";
    private static final String FOOTER =
    "</innerrecords>\n"
    + "</outer>\n";
       
    private final static String THREE_RECORDS =
    HEADER
    + "<record id=\"recordA\">\n"
    + "<mycontent atag=\"tagvalueA\">contentA</mycontent>\n"
    + "<morecontent><subcontent>subA</subcontent></morecontent>\n"
    + "</record>\n"

    + "<record id=\"recordB\">\n"
    + "<mycontent atag=\"tagvalueB\">contentB</mycontent>\n"
    + "<morecontent><subcontent>subB</subcontent></morecontent>\n"
    + "</record>\n"

    + "<record id=\"recordC\">\n"
    + "<mycontent atag=\"tagvalueC\">contentC</mycontent>\n"
    + "<morecontent><subcontent>subC</subcontent></morecontent>\n"
    + "</record>\n"
    + FOOTER;

    private final static String TWO_RECORDS =
    HEADER
    + "<record id=\"record2A\">\n"
    + "<mycontent atag=\"tagvalue2A\">content2A</mycontent>\n"
    + "<morecontent><subcontent>sub2A</subcontent></morecontent>\n"
    + "</record>\n"

    + "<record id=\"record2B\">\n"
    + "<mycontent atag=\"tagvalue2B\">content2B</mycontent>\n"
    + "<morecontent><subcontent>sub2B</subcontent></morecontent>\n"
    + "</record>\n"
    + FOOTER;

    private final static String NO_RECORDS = HEADER + FOOTER;

    private final static String INVALID_RECORDS = "Not XML!";

    private final static int NUM_RECORDS = 5;

    @Override
    public void setUp () throws Exception {
        super.setUp();
        if (sourceRoot.exists()) {
            Files.delete(sourceRoot);
        }
        assertTrue("The root '" + sourceRoot + "' should be created",
                   sourceRoot.mkdirs());
        Files.saveString(THREE_RECORDS, new File(sourceRoot, "three.xml"));
        Files.saveString(TWO_RECORDS, new File(sourceRoot, "two.xml"));
        Files.saveString(NO_RECORDS, new File(sourceRoot, "none.xml"));
        Files.saveString(INVALID_RECORDS, new File(sourceRoot, "invalid.xml"));
        Files.saveString("Dummy content", new File(sourceRoot, "dummy.dummy"));
        ReleaseHelper.cleanup();
    }
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Files.delete(sourceRoot);
        ReleaseHelper.cleanup();
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final File sourceRoot =
            new File(new File(System.getProperty("java.io.tmpdir")),
                     "IngestTest");

    public void testBasicLoad() throws Exception {
        ArchiveReader reader = new ArchiveReader(
            ReleaseHelper.getArchiveReaderConfiguration(sourceRoot.toString()));
        for (int i = 0 ; i < 4 ; i++) {
            assertTrue("Reader should have next for file #" + (i+1),
                       reader.hasNext());
            assertNotNull("The next should give a payload", reader.next());
        }
        assertFalse("Reader should only have 4 payloads",
                   reader.hasNext());
        reader.close(true);
    }

    private static final String TESTBASE = "testbase";
    public void testRecordExtraction() throws Exception {
        ArchiveReader reader = new ArchiveReader(
            ReleaseHelper.getArchiveReaderConfiguration(sourceRoot.toString()));
        Configuration splitterConf = getSplitterConfiguration();

        XMLSplitterFilter splitter = new XMLSplitterFilter(splitterConf);
        splitter.setSource(reader);
        for (int i = 0 ; i < NUM_RECORDS ; i++) {
            assertTrue("Splitter should have next for record #" + (i+1),
                       splitter.hasNext());
            Payload payload = splitter.next();
            assertNotNull("The next should give a payload", payload);
        }
        assertFalse("Splitter should only have " + NUM_RECORDS + " payloads",
                   splitter.hasNext());
        splitter.close(true);
    }

    public static Configuration getSplitterConfiguration() {
        return Configuration.newMemoryBased(
            XMLSplitterFilter.CONF_BASE, TESTBASE,
            XMLSplitterFilter.CONF_COLLAPSE_PREFIX, "true",
            XMLSplitterFilter.CONF_ID_ELEMENT, "record#id",
            XMLSplitterFilter.CONF_ID_PREFIX, "myprefix",
            XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, "true",
            XMLSplitterFilter.CONF_RECORD_ELEMENT, "record",
            XMLSplitterFilter.CONF_REQUIRE_VALID, "false");
    }

    /*
     * @return a configuration for a FileReader that looks for data in
     * {@link #sourceRoot}.
     * @deprecated in favor of {@link ReleaseHelper#getArchiveReaderConfiguration(String)}.
     */
/*    public static Configuration getReaderConfiguration() {
        Configuration readerConf = Configuration.newMemoryBased();
        readerConf.set(FileReader.CONF_ROOT_FOLDER, sourceRoot.toString());
        readerConf.set(FileReader.CONF_RECURSIVE, true);
        readerConf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        readerConf.set(FileReader.CONF_COMPLETED_POSTFIX, ".processed");
        return readerConf;
    }*/

    //public static final String STORAGE_ADDRESS =
//            "//localhost:28000/summa-storage";

    public void testStorage() throws Exception {
        Configuration storageConf = ReleaseHelper.getStorageConfiguration("foo");
        assertTrue("Storage conf should have location", storageConf.valueExists(
                DatabaseStorage.CONF_LOCATION));
        Storage storage = StorageFactory.createStorage(storageConf);

        long iterKey = storage.getRecordsModifiedAfter(0, TESTBASE, null);
        Iterator<Record> iterator = new StorageIterator(storage, iterKey);
        assertFalse("The Storage should be empty", iterator.hasNext());

        Record record = new Record("foo", TESTBASE, new byte[0]);
        storage.flush(record);

        iterKey = storage.getRecordsModifiedAfter(0, TESTBASE, null);
        iterator = new StorageIterator(storage, iterKey);
        assertTrue("The Storage should contain something", iterator.hasNext());
        assertEquals("The first record should have id as expected",
                     "foo", iterator.next().getId());
        assertFalse("After extraction of a Record, "
                    + "the iterator should be empty",
                   iterator.hasNext());
        storage.close();
    }

    public void testStorageClients() throws Exception {
        String STORAGE_NAME = "remote";
        Storage storage = StorageFactory.createStorage(
            ReleaseHelper.getStorageConfiguration(STORAGE_NAME));

        StorageReaderClient reader = new StorageReaderClient(
            ReleaseHelper.getStorageClientConfiguration(STORAGE_NAME));

        long iterKey = reader.getRecordsModifiedAfter(0, TESTBASE, null);
        Iterator<Record> iterator = new StorageIterator(storage, iterKey);
        assertFalse("The Storage should be empty", iterator.hasNext());

        StorageWriterClient writer = new StorageWriterClient(
            ReleaseHelper.getStorageClientConfiguration(STORAGE_NAME));
        Record record = new Record("foo", TESTBASE, new byte[0]);
        writer.flush(record);

        iterKey = reader.getRecordsModifiedAfter(0, TESTBASE, null);
        iterator = new StorageIterator(storage, iterKey);
        assertTrue("The Storage should contain something", iterator.hasNext());
        assertEquals("The first record should have id as expected",
                     "foo", iterator.next().getId());
        assertFalse("After extraction of a Record, "
                    + "the iterator should be empty",
                   iterator.hasNext());
    }

    // See StorageServiceTest for RecordWriter unit test

    public void testIngestWorkflow() throws Exception {
        final String STORAGE_NAME = "ingestflow";

        // Plain ingest
        Storage storage = ReleaseHelper.startStorage(STORAGE_NAME);
        int ingested = ReleaseHelper.ingest(
            STORAGE_NAME, sourceRoot.toString(), TESTBASE,
            "myprefix", "record", "record#id");
        assertEquals("The expected number of Records should be ingested",
                     NUM_RECORDS, ingested);

        // Explicit check for existence in Storage
        List<Record> records = ReleaseHelper.getRecords(STORAGE_NAME);
        assertEquals(
            "There should be the expected number of records in Storage",
            NUM_RECORDS, records.size());

        storage.close();
        log.debug("Finished testRemote unit test");
    }


    // with proper use of FilterChain
    public void testFilterServiceWorkflow() throws Exception {
        final String STORAGE_NAME = "serviceflow_storage";

        File dataLocation = new File(Resolver.getURL(
                "5records").getFile());
        assertTrue("The test-data should be present at " + dataLocation,
                   dataLocation.exists());

        int TIMEOUT = 10000;

        Storage storage = ReleaseHelper.startStorage(STORAGE_NAME);

        File filterConfFile = new File(Resolver.getURL(
                "5records/filter_setup.xml").getFile());
        assertTrue("The filter conf. '" + filterConfFile + "' should exist",
                   filterConfFile.exists());
        Configuration filterConf = Configuration.load(filterConfFile.getPath());
        filterConf.getSubConfigurations(FilterControl.CONF_CHAINS).get(0).
                getSubConfigurations(FilterSequence.CONF_FILTERS).get(0).
                set(ArchiveReader.CONF_ROOT_FOLDER, dataLocation.getPath());
        assertNotNull(
            "Configuration should contain " + FilterControl.CONF_CHAINS,
            filterConf.getSubConfigurations(FilterControl.CONF_CHAINS));

        FilterService ingester = new FilterService(filterConf);
        ingester.start();

        long endTime = System.currentTimeMillis() + TIMEOUT;
        while (!ingester.getStatus().getCode().equals(Status.CODE.stopped) &&
               System.currentTimeMillis() < endTime) {
            log.debug("Sleeping a bit");
            Thread.sleep(100);
        }
        assertEquals("The ingester should have stopped by now",
                     Status.CODE.stopped, ingester.getStatus().getCode());

        long iterKey = storage.getRecordsModifiedAfter(0, TESTBASE, null);
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

        storage.close();
    }
}
