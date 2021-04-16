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
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.FakeStorage;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorageTest;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordReaderTest extends TestCase {
    private static Log log = LogFactory.getLog(RecordReaderTest.class);
    private static File db = new File("target/test_result", "summatest" + File.separator + "recordreadertest");
    private final int timeout = 1000;

    public RecordReaderTest(String name) {
        super(name);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (db.exists()) {
            Files.delete(db);
        }
        db.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // Clean up and prepare a fresh directory
        //db.mkdirs();
        Files.delete(db);
    }

    public static Test suite() {
        return new TestSuite(RecordReaderTest.class);
    }

    // Integration test: Only directly usable for toes@kb.dk - might be used for inspiration for at proper unit test
    public void disabledtestDOMSIntegration() throws IOException {
        File TMP = File.createTempFile("SummaRise-doms-integration", ".xml");
        
        TMP.deleteOnExit();
        RecordReader reader = new RecordReader(Configuration.newMemoryBased(
                RecordReader.CONF_START_FROM_SCRATCH, false,
                RecordReader.CONF_PROGRESS_FILE, TMP.getAbsolutePath(),
                RecordReader.CONF_BASE, "doms_radioTVCollection",
                RecordReader.CONF_EXPAND_CHILDREN, true,
                RecordReader.CONF_EXPAND_PARENTS, false,
                ConnectionConsumer.CONF_RPC_TARGET, "localhost:57300/doms-storage-proxy"
        ));
    }

    public void testIterator() throws IOException {
        int RECORDS = 10;
        int BATCH = 4;

        List<Record> records = new ArrayList<>(RECORDS);
        for (int i = 0 ; i < RECORDS ; i++) {
            records.add(new Record("record_" + i, "dummy", new byte[0]));
        }
        Storage storage = new FakeStorage(records, BATCH);

        RMIStorageProxy rmiStorage = new RMIStorageProxy(Configuration.newMemoryBased(
                RMIStorageProxy.CONF_SERVICE_NAME, "Faker",
                RMIStorageProxy.CONF_SERVICE_PORT, 28000
        ), storage);

        assertEquals("Without partial deliveries, only a subset should be returned", BATCH, countRecords(false));
        assertEquals("With partial deliveries, all Records should be returned", RECORDS, countRecords(true));

        rmiStorage.close();
    }

    public void testParentInclusion() throws Exception {
        Storage storage = new H2Storage(DatabaseStorageTest.createConf());
        {
            Record orphan = new Record("orphan", "dummy", new byte[0]);
            Record parent = new Record("parent", "dummy", new byte[0]);
            Record child = new Record("child", "dummy", new byte[0]);
            child.setParentIds(Arrays.asList("parent"));

            storage.flushAll(Arrays.asList(orphan, parent, child));
        }
        RMIStorageProxy rmiStorage = new RMIStorageProxy(Configuration.newMemoryBased(
                RMIStorageProxy.CONF_SERVICE_NAME, "Faker",
                RMIStorageProxy.CONF_SERVICE_PORT, 28000
        ), storage);
        { // Direct lookup with parent resolving
            Record extracted = rmiStorage.getRecord("child", new QueryOptions(
                    false, false, 1, 1, null, new QueryOptions.ATTRIBUTES[]{
                    QueryOptions.ATTRIBUTES.PARENTS,
                    QueryOptions.ATTRIBUTES.BASE,
                    QueryOptions.ATTRIBUTES.CONTENT,
                    QueryOptions.ATTRIBUTES.CREATIONTIME,
                    QueryOptions.ATTRIBUTES.DELETED,
                    QueryOptions.ATTRIBUTES.HAS_RELATIONS,
                    QueryOptions.ATTRIBUTES.ID,
                    QueryOptions.ATTRIBUTES.INDEXABLE,
                    QueryOptions.ATTRIBUTES.META,
                    QueryOptions.ATTRIBUTES.MODIFICATIONTIME
            }));
            assertNotNull("The RMI call should extract a Record",
                         extracted);
            assertNotNull("The RMI extracted record should have a parent ID",
                         extracted.getParentIds());
            assertEquals("The RMI extracted record should have the right parent ID",
                         "parent", extracted.getParentIds().get(0));
            assertNotNull("The RMI extracted record should have a parent",
                          extracted.getParents());
            assertEquals("The RMI extracted record should have the right parent",
                         "parent", extracted.getParents().get(0).getId());
        }

        { // Resolve parents
            List<Record> extracted = empty(Configuration.newMemoryBased(
                    RecordReader.CONF_ALLOW_PARTIAL_DELIVERIES, true,
                    RecordReader.CONF_START_FROM_SCRATCH, true,
                    RecordReader.CONF_EXPAND_CHILDREN, false,
                    RecordReader.CONF_EXPAND_PARENTS, true,
                    ConnectionConsumer.CONF_RPC_TARGET, "//localhost:28000/Faker"
            ));
            for (Record record: extracted) {
                if ("child".equals(record.getId())) {
                    assertNotNull("RecordReader child should have a parent ID", record.getParentIds());
                    assertEquals("RecordReader child should have the correct parent ID", "parent",
                                 record.getParentIds().get(0));
                    assertNotNull("RecordReader record should have a parent Record", record.getParents());
                    assertEquals("RecordReader record should have the correct parent", "parent",
                                 record.getParents().get(0).getId());
                }
            }
        }

        { // Do not resolve parents
            List<Record> extracted = empty(Configuration.newMemoryBased(
                    RecordReader.CONF_ALLOW_PARTIAL_DELIVERIES, true,
                    RecordReader.CONF_START_FROM_SCRATCH, true,
                    RecordReader.CONF_EXPAND_CHILDREN, false,
                    RecordReader.CONF_EXPAND_PARENTS, false,
                    ConnectionConsumer.CONF_RPC_TARGET, "//localhost:28000/Faker"
            ));
            for (Record record: extracted) {
                if ("record".equals(record.getId())) {
                    assertNotNull("Non-resolving record should have a parent ID", record.getParentIds());
                    assertEquals("Non-resolving record should have the correct parent ID", "parent",
                                 record.getParentIds().get(0));
                    assertNull("Non-resolving record should not have a parent Record", record.getParents());
                }
            }
        }
        rmiStorage.close();

    }

    private int countRecords(boolean allowPartialDeliveries) throws IOException {
        RecordReader reader = new RecordReader(Configuration.newMemoryBased(
                RecordReader.CONF_ALLOW_PARTIAL_DELIVERIES, allowPartialDeliveries,
                RecordReader.CONF_START_FROM_SCRATCH, true,
                ConnectionConsumer.CONF_RPC_TARGET, "//localhost:28000/Faker"
        ));

        int count = 0;
        while (reader.pump()) {
            count++;
        }
        return count;
    }


    public static Storage createStorage() throws Exception {
        Configuration conf = Configuration.newMemoryBased(DatabaseStorage.CONF_LOCATION, db.getAbsolutePath());

        return StorageFactory.createStorage(conf);
    }

    public void testTimestampFormatting() throws Exception {
        Calendar t = new GregorianCalendar(2008, 3, 17, 21, 50, 57);
        Assert.assertEquals("The timestamp should be properly formatted", expected,
                            String.format(Locale.ROOT, ProgressTracker.TIMESTAMP_FORMAT, t));
    }

    public void waitForHasNext(RecordReader r, long timeout) throws Exception {
        long start = System.currentTimeMillis();

        while (!r.hasNext()) {
            Thread.sleep(100);
            if (System.currentTimeMillis() - start > timeout) {
                fail("RecordReader did not have any records before timeout");
            }
        }
        log.debug("RecordReader has next");
    }

   /*public void testTimestampExtraction() throws Exception {
        Calendar t = new GregorianCalendar(2008, 3, 17, 21, 50, 57);
        long expectedMS = t.getTimeInMillis();
        String formatted = String.format(Locale.ROOT, RecordReader.TIMESTAMP_FORMAT, t);
        assertEquals("Parsing the formatted timestamp-containing text should "
                     + "match the expected point in time",
                     expectedMS, RecordReader.getTimestamp(new File("foo"), 
                                                           formatted));
    }*/

    private static final String expected = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                                           "<lastRecordTimestamp>\n" +
                                           "<epoch>1208461857000</epoch>\n" +
                                           "<!-- As of 2013-05-01, iso is the authoritative timestamp and epoch is deprecated -->\n" +
                                           "<iso>20080417-215057.000</iso>\n" +
                                           "</lastRecordTimestamp>\n";

    public void testPatternMatching() throws Exception {
        Pattern p = Pattern.compile("([0-9]{4})([0-9]{2})([0-9]{2})-([0-9]{2})([0-9]{2})([0-9]{2})");
        assertTrue("Pattern should match simple case", p.matcher("20080417-215057").matches());

        //String TAG = "lastRecordTimestamp";
        Pattern pfull;/* = Pattern.compile(".*<" + TAG + ">"
                    + "([0-9]{4})([0-9]{2})([0-9]{2})-"
                    + "([0-9]{2})([0-9]{2})([0-9]{2})"
                    + "</" + TAG + ">", Pattern.DOTALL);*/
        pfull = ProgressTracker.TIMESTAMP_PATTERN;
        assertTrue("Pattern should match extended case with input " + expected, pfull.matcher(expected).matches());
        assertTrue("Pattern should match full case", pfull.matcher(expected).matches());
    }

    public void testOne() throws Exception {
        Storage sto = createStorage();
        RecordReader r = new RecordReader(Configuration.newMemoryBased());
        Record orig = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        sto.flush(orig);
        //System.exit(1);
        waitForHasNext(r, timeout);

        Payload p = r.next();
        Record rec = p.getRecord();

        assertEquals(
                "\nVerbose orig: " + orig.toString(true) + "\nVerbose rec : " + rec.toString(true) + "\n", orig, rec);
        r.close(true);
        sto.close();
    }

    public void testTwo() throws Exception {
        Storage sto = createStorage();
        RecordReader r = new RecordReader(Configuration.newMemoryBased());

        Record orig1 = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        Record orig2 = new Record("test2", "base", "Hello".getBytes(StandardCharsets.UTF_8));

        sto.flushAll(Arrays.asList(orig1, orig2));
        waitForHasNext(r, timeout);

        Payload p = r.next();
        Record rec = p.getRecord();
        assertEquals(orig1, rec);

        p = r.next();
        rec = p.getRecord();
        assertEquals(orig2, rec);
        r.close(true);
        sto.close();
    }

    public void testThree() throws Exception {
        Storage sto = createStorage();
        RecordReader r = new RecordReader(Configuration.newMemoryBased());

        Record orig1 = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        Record orig2 = new Record("test2", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        Record orig3 = new Record("test3", "base", "Hello".getBytes(StandardCharsets.UTF_8));

        sto.flushAll(Arrays.asList(orig1, orig2, orig3));
        waitForHasNext(r, timeout);

        Payload p = r.next();
        Record rec = p.getRecord();
        assertEquals(orig1, rec);

        p = r.next();
        rec = p.getRecord();
        assertEquals(orig2, rec);

        p = r.next();
        rec = p.getRecord();
        assertEquals(orig3, rec);
        r.close(true);
        sto.close();
    }

    public void testUpdateDefault() throws Exception {
        testUpdate(Configuration.newMemoryBased());
    }

    public void testUpdateCustomProgress() throws Exception {
        testUpdate(Configuration.newMemoryBased(RecordReader.CONF_PROGRESS_FILE, "delete_progress.xml"));
    }

    public void testUpdate(Configuration readerConf) throws Exception {
        Storage sto = createStorage();

        try {
            Record orig1 = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
            Record orig2 = new Record("test2", "base", "Hello".getBytes(StandardCharsets.UTF_8));
            Record orig3 = new Record("test3", "base", "Hello".getBytes(StandardCharsets.UTF_8));

            sto.flushAll(Arrays.asList(orig1, orig2));
            { // Implicit initial
                assertEquals("Initial iteration", Arrays.asList(orig1, orig2), empty(readerConf));
            }

            sto.flush(orig3);
            { // Update with content
                assertEquals("First update", Arrays.asList(orig3), empty(readerConf));
            }

            { // Update without content
                assertEquals("No update since last empty", new ArrayList<Record>(), empty(readerConf));
            }

            { // Full request
                boolean originalStartFromScratch = readerConf.getBoolean(RecordReader.CONF_START_FROM_SCRATCH, false);
                readerConf.set(RecordReader.CONF_START_FROM_SCRATCH, true);
                assertEquals("Start from scratch", Arrays.asList(orig1, orig2, orig3), empty(readerConf));
                readerConf.set(RecordReader.CONF_START_FROM_SCRATCH, originalStartFromScratch);
            }

            sto.flush(orig2);
            { // Update existing
                assertEquals("Update existing", Arrays.asList(orig2), empty(readerConf));
            }
        } finally {
            sto.close();
        }
    }

    public void testUpdateContinueOffset() throws Exception {
        Storage sto = createStorage();
        Configuration clear = Configuration.newMemoryBased(
                RecordReader.CONF_PROGRESS_FILE, "delete_progress.xml",
                RecordReader.CONF_START_FROM_SCRATCH, true
        );
        Configuration none = Configuration.newMemoryBased(
                RecordReader.CONF_PROGRESS_FILE, "delete_progress.xml"
        );
        Configuration back = Configuration.newMemoryBased(
                RecordReader.CONF_PROGRESS_FILE, "delete_progress.xml",
                RecordReader.CONF_CONTINUE_OFFSET, -900L
        );

        try {
            Record orig1 = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
            Record orig2 = new Record("test2", "base", "Hello".getBytes(StandardCharsets.UTF_8));
            Record orig3 = new Record("test3", "base", "Hello".getBytes(StandardCharsets.UTF_8));

            sto.flush(orig1);
            Thread.sleep(500);
            sto.flush(orig2);
            Thread.sleep(500);
            { // Implicit initial
                assertEquals("Initial iteration", Arrays.asList(orig1, orig2), empty(clear));
            }
            
            { // Update without content
                assertEquals("No update since initial iteration", new ArrayList<Record>(), empty(none));
            }

            sto.flush(orig3);
            { // Update with content
                assertEquals("First update", Collections.singletonList(orig3), empty(none));
            }

            { // Update without content
                assertEquals("No update since last empty", new ArrayList<Record>(), empty(none));
            }

            { // Update with negative origo
                assertEquals("Checking back in time 1", Arrays.asList(orig2, orig3), empty(back));
            }

            { // Update with negative origo
                assertEquals("Checking back in time 2", Arrays.asList(orig2, orig3), empty(back));
            }

            {
                assertEquals("Checking back in time, no offset", Collections.emptyList(), empty(none));
            }
            { // Update with negative origo
                assertEquals("Checking back in time with offset",
                             Arrays.asList(orig2, orig3), empty(back));
            }
        } finally {
            sto.close();
        }
    }

    public void testNoUpdates() throws Exception {
        Storage sto = createStorage();
        Configuration readerConf = Configuration.newMemoryBased();
        try {
            Record orig1 = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
            Record orig2 = new Record("test2", "base", "Hello".getBytes(StandardCharsets.UTF_8));

            sto.flushAll(Arrays.asList(orig1, orig2));
            { // Implicit initial
                assertEquals("Initial iteration", Arrays.asList(orig1, orig2), empty(readerConf));
            }

            { // No news
                assertEquals("Second iteration (with no available updates)", new ArrayList<Record>(),
                             empty(readerConf));
            }
        } finally {
            sto.close();
        }
    }

    private void assertEquals(String message, List<Record> expected, List<Record> actual) {
        assertEquals(message + ". There should be the right number of Records", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(
                    message + ". The Records at position " + i + " should be equal", expected.get(i), actual.get(i));
        }
    }

    private List<Record> empty(Configuration readerConf) throws IOException {
        RecordReader reader = new RecordReader(readerConf);
        List<Record> records = new ArrayList<>();
        while (reader.hasNext()) {
            records.add(reader.next().getRecord());
        }
        reader.close(true);
        return records;
    }

    public void testDelete() throws Exception {
        Storage sto = createStorage();
        RecordReader r = new RecordReader(Configuration.newMemoryBased());

        Record orig1 = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        Record orig2 = new Record("test2", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        Record orig3 = new Record("test3", "base", "Hello".getBytes(StandardCharsets.UTF_8));

        sto.flushAll(Arrays.asList(orig1, orig2, orig3));
        waitForHasNext(r, timeout);

        Payload p = r.next();
        Record rec = p.getRecord();
        assertEquals(orig1, rec);

        p = r.next();
        rec = p.getRecord();
        assertEquals(orig2, rec);

        p = r.next();
        rec = p.getRecord();
        assertEquals(orig3, rec);
        r.close(true);

        r = new RecordReader(Configuration.newMemoryBased());
        Record orig4 = new Record("test4", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        sto.flushAll(Arrays.asList(orig4));
        waitForHasNext(r, timeout);

        r = new RecordReader(Configuration.newMemoryBased());
        p = r.next();
        rec = p.getRecord();
        assertEquals(orig4, rec);
        r.close(true);

        r = new RecordReader(Configuration.newMemoryBased());
        Record orig4del = new Record("test4", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        orig4del.setDeleted(true);
        sto.flushAll(Arrays.asList(orig4del));
        waitForHasNext(r, timeout);
        p = r.next();
        rec = p.getRecord();
        assertTrue("Record 4 should now be deleted", rec.isDeleted());

        r.close(true);
        sto.close();
    }

    public void testWatchOne() throws Exception {
        Storage sto = createStorage();
        RecordReader r = new RecordReader(Configuration.newMemoryBased(RecordReader.CONF_STAY_ALIVE, true));

        // Launch the probe on an empty storage waiting for records
        RecordProbe probe = new RecordProbe(r, 1);
        Thread thread = new Thread(probe, "RecordProbe");
        thread.start();

        Record orig = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        sto.flush(orig);

        // Wait for the probe to return, 10s
        thread.join(10000);
        if (probe.records.isEmpty()) {
            fail("No records appeared on reader before timeout");
        } else if (probe.records.size() != 1) {
            fail("Too many records. Expected 1, but got " + probe.records.size());
        }

        assertEquals(orig, probe.records.get(0));
        r.close(true);
        sto.close();
    }

    public void testWatchThree() throws Exception {
        Storage sto = createStorage();
        RecordReader r = new RecordReader(Configuration.newMemoryBased(RecordReader.CONF_STAY_ALIVE, true));

        // Launch the probe on an empty storage waiting for records
        RecordProbe probe = new RecordProbe(r, 3);
        Thread thread = new Thread(probe, "RecordProbe");
        thread.start();

        Record orig1 = new Record("test1", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        Record orig2 = new Record("test2", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        Record orig3 = new Record("test3", "base", "Hello".getBytes(StandardCharsets.UTF_8));
        sto.flushAll(Arrays.asList(orig1, orig2, orig3));

        // Wait for the probe to return, 10s
        thread.join(10000);
        if (probe.records.isEmpty()) {
            fail("No records appeared on reader before timeout");
        } else if (probe.records.size() != 3) {
            fail("Too many records. Expected 3, but got " + probe.records.size());
        }

        assertEquals(orig1, probe.records.get(0));
        assertEquals(orig2, probe.records.get(1));
        assertEquals(orig3, probe.records.get(2));
        r.close(true);
        sto.close();
    }

    static class RecordProbe implements Runnable {
        RecordReader r;
        List<Record> records;
        int recordCount;

        public RecordProbe(RecordReader rr, int recordCount) {
            r = rr;
            records = new LinkedList<>();
            this.recordCount = recordCount;
        }

        @Override
        public void run() {
            for (int i = 1; i <= recordCount; i++) {
                Payload p = r.next();
                records.add(p.getRecord());
            }
        }
    }
}
