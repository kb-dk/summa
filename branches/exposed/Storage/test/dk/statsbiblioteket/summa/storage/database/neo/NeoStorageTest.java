package dk.statsbiblioteket.summa.storage.database.neo;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class NeoStorageTest extends TestCase {
    private enum STORAGE { h2, neo }

    public NeoStorageTest(String name) {
        super(name);
    }

    private File TMP = new File("NEOTMP").getAbsoluteFile();
    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TMP.exists()) {
            Files.delete(TMP);
        }
        //noinspection ResultOfMethodCallIgnored
        TMP.mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(NeoStorageTest.class);
    }

    public void testCreate() throws IOException {
        Storage ns = getStorage(STORAGE.neo);
        ns.close();
    }

    public void testFlush() throws IOException {
        Storage ns = getStorage(STORAGE.neo);
        Record record = new Record("foo", "bar", new byte[0]);
        ns.flush(record);
        ns.close();
    }

    public void testGetRecord() throws IOException {
        Storage ns = getStorage(STORAGE.neo);
        Record record = new Record("foo", "bar", new byte[0]);
        ns.flush(record);

        Record restored = ns.getRecord("foo", null);
        assertEquals("The restored record should have the right base",
                     "bar", restored.getBase());
        ns.close();
    }

    public void testChild() throws IOException {
        Storage ns = getStorage(STORAGE.neo);
        {
            Record record = new Record("foo", "bar", new byte[0]);
            record.setChildren(Arrays.asList(
                new Record("child", "zoo", new byte[0])));
            ns.flush(record);
        }

        Record restored = ns.getRecord("foo", null);
        assertEquals("The returned record should have the right child", "child",
                     restored.getChildren().get(0).getId());
        ns.close();
    }

    public void testGetRecordsModifiedAfter() throws IOException {
        long startTime = System.currentTimeMillis();
        Storage ns = getStorage(STORAGE.neo);
        Record record = new Record("foo", "bar", new byte[0]);
        ns.flush(record);

        long key = ns.getRecordsModifiedAfter(startTime - 10000, "bar", null);
        Record next = ns.next(key);
        assertEquals("The retrieved record should match base", "bar",
                     next.getBase());
        try {
            ns.next(key);
            fail("There should be no more Records");
        } catch (NoSuchElementException e) {
            // Expected
        }
        ns.close();
    }

    public void testScale10c1() throws IOException {
        testScale(10, 1, 1, STORAGE.neo);
        System.out.println("");
        testScale(10, 1, 1, STORAGE.h2);
    }

    public void testScale1Kc2() throws IOException {
        testScale(1000, 2, 100, STORAGE.neo);
        System.out.println("");
        testScale(1000, 2, 100, STORAGE.h2);
    }

    public void disabledtestScale1Kc5() throws IOException {
        testScale(1000, 5, 100, STORAGE.neo);
        System.out.println("");
        testScale(1000, 5, 100, STORAGE.h2);
    }

    public void disabledtestScale10Kc2() throws IOException {
        testScale(10000, 2, 10, STORAGE.neo);
        System.out.println("");
        testScale(10000, 2, 10, STORAGE.h2);
    }

    public void disabledtestScale10Kc5() throws IOException {
        testScale(10000, 5, 100, STORAGE.neo);
        System.out.println("");
        testScale(10000, 5, 100, STORAGE.h2);
    }

    public void disabledtestScale100Kc5b100() throws IOException {
        testScale(100000, 5, 100, STORAGE.neo);
        System.out.println("");
        testScale(100000, 5, 100, STORAGE.h2);
    }

    public void testReopen() throws IOException {
        if (TMP.exists()) {
            Files.delete(TMP);
        }
        //noinspection ResultOfMethodCallIgnored
        TMP.mkdirs();
        Storage ns = createIndex(100, 5, 100, STORAGE.neo, 100);
        ns.close();
        ns = createIndex(100, 5, 100, STORAGE.neo, 100);
        ns.close();
    }

    /*
     * With default mem (~1,2GB) - too little
     * Index: 8,8GB
     * Ingest in 1 hour, 35 minutes, 28 seconds, 859 ms with 174 records/sec
     */
    public void testScale1MKc3b100() throws IOException {
        testScale(1000000, 3, 1000, STORAGE.neo);
/*        System.out.println("");
        testScale(1000000, 3, 100, STORAGE.h2);*/
    }

    /*
     * With default mem (~1,2GB)
     * Index:18,3GB
     * Ingest in 13 minutes, 11 seconds, 17 ms with 1264 records/sec
     *
     */
    public void disabledtestScale1MKc0b100() throws IOException {
        testScale(1000000, 0, 100, STORAGE.neo);
        System.out.println("");
        testScale(1000000, 0, 100, STORAGE.h2);
    }

    public void disabledtestScale10Mc3b1000() throws IOException {
        testScale(10000000, 3, 1000, STORAGE.neo);
        System.out.println("");
        testScale(10000000, 3, 1000, STORAGE.h2);
    }

    public void testScale(
        int records, int children, int buffersize, STORAGE storage)
        throws IOException {
        long feedback = records / 10;
        long startTime = System.currentTimeMillis();
        Storage ns =
            createIndex(records, children, buffersize, storage, feedback);

        for (int i = 0 ; i < 1000 ; i++) {
            System.gc();
            performExtraction(records, children, feedback, startTime, ns);
        }

        ns.close();
    }

    private Storage createIndex(
        int records, int children, int buffersize, STORAGE storage,
        long feedback) throws IOException {
        String lastID = "foo" + (records - 1);
        Profiler ingest = new Profiler(records);
        ingest.setBpsSpan(1000);
        Storage ns = getStorage(storage);
        if (ns.getRecord(lastID, null) != null) {
            System.out.println(
                "Storage contains " + lastID + ". We assume it is "
                + "populated and do not perform further ingest");
            return ns;
        }
        System.out.println(
            "Creating " + storage + " storage with " + records + " records "
            + "with " + children + " children using commit size " + buffersize);
        List<Record> buffer = new ArrayList<Record>(buffersize);
        for (int i = 0 ; i < records ; i++) {
            if (i != 0 && i % feedback == 0) {
                System.out.println(
                    i + "/" + records + " at " + (int)ingest.getBps(true)
                    + " records/sec. ETA: " + ingest.getETAAsString(true));
            }
            byte[] content = new byte[500];
            content[0] = (byte)i;
            Record record = new Record("foo" + i, "bar", content);
            if (children > 0) {
                List<Record> childRecords = new ArrayList<Record>(children);
                for (int child = 0 ; child < children ; child++) {
                    childRecords.add(
                        new Record(record.getId() + "_child" + child,
                                   "zoo", new byte[500]));
                }
                record.setChildren(childRecords);
            }
            buffer.add(record);
            if (buffer.size() == buffersize) {
                ns.flushAll(buffer);
                buffer.clear();
            }
            ingest.beat();
        }
        if (buffer.size() > 0) {
            ns.flushAll(buffer);
            buffer.clear();
        }
        ingest.pause();
        System.out.println(
            "Finished ingest in " + ingest.getSpendTime()
            + " with " + (int)ingest.getBps(false) + " records/sec");
        if (ns.getRecord(lastID, null) == null) {
            throw new IllegalStateException(
                "Unable to extract " + lastID+ " from newly populated storage");
        }
        return ns;
    }

    private void performExtraction(
        int records, int children, long feedback, long startTime, Storage ns)
                                                            throws IOException {
        System.out.println(
            "Extracting "+ records + " records and verifying content");
        Profiler extract = new Profiler(records);
        long key = ns.getRecordsModifiedAfter(
            0, "bar", new QueryOptions(null, null, 999, 0));
        int count = 0;
        while (true) {
            try {
                Record record = ns.next(key);

                extract.beat();
                assertEquals("The Records should have keys in order",
                             "foo" + count, record.getId());
                assertEquals("The content should sanity check",
                             (byte)count, record.getContent()[0]);
                assertEquals("The number of children should match",
                             children, record.getChildren().size());
                if (count != 0 && count % feedback == 0) {
                    System.out.println(
                        count + "/" + records + " at "
                        + (int)extract.getBps(true) + " records/sec. ETA: "
                        + extract.getETAAsString(true));
                }
            } catch (NoSuchElementException e) {
                break;
            }
            count++;
        }
        assertEquals("The number of extracted records should match ingested",
                     records, count);
        extract.pause();
        System.out.println(
            "Finished extraction in " + extract.getSpendTime()
            + " with " + (int)extract.getBps(false) + " records/sec");
    }

    private Storage getStorage(STORAGE storage) throws IOException {
        Configuration conf = Configuration.newMemoryBased(
            DatabaseStorage.CONF_LOCATION, TMP.getAbsolutePath(),
            DatabaseStorage.CONF_CREATENEW, true,
            DatabaseStorage.CONF_FORCENEW, true
        );
        switch (storage) {
            case h2:  return new H2Storage(conf);
            case neo: return new NeoStorage(conf);
            default: throw new UnsupportedOperationException(
                "Storage " + storage + " is unknown");
        }
    }

}
