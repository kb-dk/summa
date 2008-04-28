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
import java.io.IOException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.FilterControl;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.ingest.stream.XMLSplitterFilter;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.service.StorageService;
import dk.statsbiblioteket.summa.control.service.FilterService;
import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.summa.storage.filter.RecordWriter;
import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.storage.io.Control;
import dk.statsbiblioteket.summa.storage.io.RecordIterator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
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

    private static int storageCounter = 0;

    public void setUp () throws Exception {
        super.setUp();
        if (sourceRoot.exists()) {
            Files.delete(sourceRoot);
        }
        assertTrue("The root '" + sourceRoot + "' should have be created",
                   sourceRoot.mkdirs());
        Files.saveString(THREE_RECORDS, new File(sourceRoot, "three.xml"));
        Files.saveString(TWO_RECORDS, new File(sourceRoot, "two.xml"));
        Files.saveString(NO_RECORDS, new File(sourceRoot, "none.xml"));
        Files.saveString(INVALID_RECORDS, new File(sourceRoot, "invalid.xml"));
        Files.saveString("Dummy content", new File(sourceRoot, "dummy.dummy"));
        deleteOldStorages();
    }
    public void tearDown() throws Exception {
        super.tearDown();
        //Files.delete(sourceRoot);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final File sourceRoot =
            new File(new File(System.getProperty("java.io.tmpdir")),
                     "IngestTest");

    public static void deleteOldStorages() throws Exception {
        for (int i = 0 ; i < 40 ; i++) { // Up to the number of distinct tests
            File attempDelete =
                    new File(System.getProperty("java.io.tmpdir"),
                             "storage" + i);
            if (attempDelete.exists()) {
                try {
                    Files.delete(attempDelete);
                    log.debug("Deleted '" + attempDelete + "'");
                } catch (IOException e) {
                    log.warn("Could not delete '" + attempDelete + "'");
                }
            }
        }
    }

    public static File getStorageLocation() {
        return new File(System.getProperty("java.io.tmpdir"),
                        "storage" + storageCounter++);
    }

    public void testBasicLoad() throws Exception {
        Configuration conf = getReaderConfiguration();
        FileReader reader = new FileReader(conf);
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
        Configuration readerConf = getReaderConfiguration();
        Configuration splitterConf = getSplitterConfiguration();

        FileReader reader = new FileReader(readerConf);
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
    }

    public static Configuration getSplitterConfiguration() {
        Configuration splitterConf = Configuration.newMemoryBased();
        splitterConf.set(XMLSplitterFilter.CONF_BASE, TESTBASE);
        splitterConf.set(XMLSplitterFilter.CONF_COLLAPSE_PREFIX, "true");
        splitterConf.set(XMLSplitterFilter.CONF_ID_ELEMENT, "record#id");
        splitterConf.set(XMLSplitterFilter.CONF_ID_PREFIX, "myprefix");
        splitterConf.set(XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, "true");
        splitterConf.set(XMLSplitterFilter.CONF_RECORD_ELEMENT, "record");
        splitterConf.set(XMLSplitterFilter.CONF_REQUIRE_VALID, "false");
        return splitterConf;
    }
    public static Configuration getReaderConfiguration() {
        Configuration readerConf = Configuration.newMemoryBased();
        readerConf.set(FileReader.CONF_ROOT_FOLDER, sourceRoot.toString());
        readerConf.set(FileReader.CONF_RECURSIVE, true);
        readerConf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        readerConf.set(FileReader.CONF_COMPLETED_POSTFIX, ".processed");
        return readerConf;
    }
    public static final String STORAGE_ADDRESS =
            "//localhost:27000/TestStorage";
    public static Configuration getStorageConfiguration() {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseControl.PROP_LOCATION,
                 getStorageLocation().toString());
        conf.set(Service.SERVICE_PORT, 27003);
        conf.set(Service.REGISTRY_PORT, 27000);
        conf.set(Service.SERVICE_ID, "TestStorage");
        System.setProperty(Service.SERVICE_ID, "TestStorage");
        return conf;
    }
    private Configuration getWriterConfiguration() {
        Configuration writerConf = Configuration.newMemoryBased();
        writerConf.set(RecordWriter.CONF_STORAGE, STORAGE_ADDRESS);
        return writerConf;
    }

    public void testStorage() throws Exception {
        Configuration storageConf = getStorageConfiguration();
        assertTrue("Storage conf should have location", storageConf.valueExists(
                DatabaseControl.PROP_LOCATION));
        Control control = StorageFactory.createController(storageConf);

        RecordIterator iterator = control.getRecordsModifiedAfter(0, TESTBASE);
        assertFalse("The Storage should be empty", iterator.hasNext());

        Record record = new Record("foo", TESTBASE, new byte[0]);
        control.flush(record);

        iterator = control.getRecordsModifiedAfter(0, TESTBASE);
        assertTrue("The Storage should contain something", iterator.hasNext());
        assertEquals("The first record should have id as expected",
                     "foo", iterator.next().getId());
        assertFalse("After extraction of a Record, "
                    + "the iterator should be empty",
                   iterator.hasNext());
    }

    // See StorageServiceTest for RecordWriter unit test

    public void testIngestWorkflow() throws Exception {
        // TODO: Implement this. Remember to use a proper filter chain
        Configuration readerConf = getReaderConfiguration();
        Configuration splitterConf = getSplitterConfiguration();
        Configuration writerConf = getWriterConfiguration();
        Configuration storageConf = getStorageConfiguration();

        // Start the Storage service remotely
        new StorageService(storageConf);
        ConnectionFactory<Service> serviceCF =
                new RMIConnectionFactory<Service>();
        ConnectionManager<Service> serviceCM =
                new ConnectionManager<Service>(serviceCF);
        ConnectionContext<Service> serviceContext =
                serviceCM.get(STORAGE_ADDRESS);
        assertNotNull("The ConnectionManager should return a Storage Service"
                      + " ConnectionContext", serviceContext);
        Service serviceRemote = serviceContext.getConnection();
        serviceRemote.start();

        // Set up filter chain
        FileReader reader = new FileReader(readerConf);
        XMLSplitterFilter splitter = new XMLSplitterFilter(splitterConf);
        splitter.setSource(reader);
        RecordWriter writer = new RecordWriter(writerConf);
        writer.setSource(splitter);

        assertTrue("The writer should have at least one record available",
                   writer.hasNext());
        for (int i = 0 ; i < NUM_RECORDS ; i++) {
            assertTrue("Writer should have next for record #" + (i+1),
                       writer.hasNext());
            Payload payload = writer.next();
            assertNotNull("The next should give a payload", payload);
        }
        assertFalse("Writer should only have " + NUM_RECORDS + " payloads",
                   writer.hasNext());
        log.debug("Play nice and close with success");
        writer.close(true);

        // Connect to the Storage remotely
        ConnectionFactory<Access> cf = new RMIConnectionFactory<Access>();
        ConnectionManager<Access> cm = new ConnectionManager<Access>(cf);

        // Do this for each connection
        ConnectionContext<Access> ctx = cm.get(STORAGE_ADDRESS);
        assertNotNull("The ConnectionManager should return an Access"
                      + " ConnectionContext", ctx);
        Access remoteStorage = ctx.getConnection();

        RecordIterator recordIterator =
                remoteStorage.getRecordsModifiedAfter(0, TESTBASE);
        assertTrue("The iterator should have at least one element",
                   recordIterator.hasNext());
        for (int i = 0 ; i < NUM_RECORDS ; i++) {
            assertTrue("Storage should have next for record #" + (i+1),
                       recordIterator.hasNext());
            Record record = recordIterator.next();
            assertNotNull("The next should give a record", record);
        }
        assertFalse("After " + NUM_RECORDS + " Records, iterator should finish",
                    recordIterator.hasNext());

        log.debug("Releasing remoteStorage connection context");
        cm.release(ctx);
        log.debug("Stopping remote service");
        serviceRemote.stop();
        log.debug("Releasing service connection context");
        serviceCM.release(serviceContext);
        log.debug("Finished testRemote unit test");
    }

    // with proper use of FilterChain
    public void testFullIngestWorkflow() throws Exception {
        System.out.println("Note: This is a pseudo-unit-test as it requires "
                           + "that the test-folder 5records are copied to "
                           + "/tmp/summatest/data");

        int TIMEOUT = 10000;

        // Storage
        Configuration storageConf = getStorageConfiguration();
        StorageService storage = new StorageService(storageConf);
        storage.start();

        // FIXME: Use classloader to locate the test root
        File filterConfFile = new File("Control/test/data/5records/"
                                       + "filter_setup.xml").getAbsoluteFile();
        assertTrue("The filter conf. '" + filterConfFile + "' should exist",
                   filterConfFile.exists());
        Configuration filterConf = Configuration.load(filterConfFile.getPath());
        assertNotNull("Configuration should contain "
                      + FilterControl.CONF_CHAINS,
                      filterConf.getString(FilterControl.CONF_CHAINS));

        FilterService ingester = new FilterService(filterConf);
        ingester.start();

        long endTime = System.currentTimeMillis() + TIMEOUT;
        while (!ingester.getStatus().getCode().equals(Status.CODE.stopped) &&
               System.currentTimeMillis() < endTime) {
            log.debug("Sleeping a bit");
            Thread.sleep(100);
        }
        assertTrue("The ingester should have stopped by now",
                   ingester.getStatus().getCode().equals(Status.CODE.stopped));

        RecordIterator recordIterator =
                storage.getRecordsModifiedAfter(0, TESTBASE);
        assertTrue("The iterator should have at least one element",
                   recordIterator.hasNext());
        for (int i = 0 ; i < NUM_RECORDS ; i++) {
            assertTrue("Storage should have next for record #" + (i+1),
                       recordIterator.hasNext());
            Record record = recordIterator.next();
            assertNotNull("The next should give a record", record);
        }
        assertFalse("After " + NUM_RECORDS + " Records, iterator should finish",
                    recordIterator.hasNext());
    }
}
