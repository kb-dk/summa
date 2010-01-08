package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.storage.api.BatchJobTest
 *
 * @author mke
 * @since Jan 7, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BatchJobTest extends TestCase {

    private static Log log = LogFactory.getLog(BatchJobTest.class);

    Storage storage;

    static String testDBRoot = "test_db";
    static String dbPrefix = "db";
    static String testBase1 = "foobar";
    static String testBase2 = "frobnibar";
    static String testId1 = "testId1";
    static String testId2 = "testId2";
    static String testId3 = "testId3";
    static String testId4 = "testId4";
    static int storageCounter = 0;
    static byte[] testContent1 = new byte[] {'s', 'u', 'm', 'm', 'a'};
    static byte[] testContent2 = new byte[] {'b', '0', 'r', 'k'};
    long testStartTime;

    private static String lastStorageLocation = null;
    public static Configuration createConf () throws Exception {

        lastStorageLocation =
                testDBRoot + File.separator + dbPrefix + (storageCounter++);
        // H2 Config
        Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, lastStorageLocation
        );

        // Derby Config
        /*Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS,
                DerbyStorage.class,
                DatabaseStorage.CONF_LOCATION,
                testDBRoot + File.separator + dbPrefix + (storageCounter++)
        );*/

        // Postgres Config
        /*Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS,
                PostgresStorage.class,
                DatabaseStorage.CONF_LOCATION,
                testDBRoot + File.separator + dbPrefix + (storageCounter++),
                DatabaseStorage.CONF_FORCENEW,
                true,
                DatabaseStorage.CONF_DATABASE,
                "summa",
                DatabaseStorage.CONF_USERNAME,
                "${user.name}",
                DatabaseStorage.CONF_PASSWORD,
                "",
                DatabaseStorage.CONF_HOST,
                ""
        );*/

        return conf;
    }

    public void setUp () throws Exception {
        File dbRoot = new File(testDBRoot);

        if (dbRoot.exists()) {
            Files.delete (dbRoot);
        }

        storage = StorageFactory.createStorage(createConf());

        /* We get spurious errors where the connection to the db isn't ready
         * when running the unit tests in batch mode */
        Thread.sleep(200);

        testStartTime = System.currentTimeMillis();
    }

    public void tearDown () throws Exception {
        log.info("Test case tear down commencing");

        if (storage instanceof DatabaseStorage) {
            ((DatabaseStorage)storage).destroyDatabase();
        }
        /* We get spurious errors where the connection to the db isn't ready
         * when running the unit tests in batch mode */
        Thread.sleep(200);

        storage.close();
        /* We get spurious errors where the connection to the db isn't ready
         * when running the unit tests in batch mode */
        Thread.sleep(200);
    }

    public void testCountByBaseJob() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        Thread.sleep(100);
        storage.flush(new Record(testId2, testBase2, testContent1));
        Thread.sleep(100);
        storage.flush(new Record(testId3, testBase1, testContent1));
        Thread.sleep(100);
        assertBaseCount(testBase1, 2);
        assertBaseCount(testBase2, 1);

        String count = storage.batchJob(
                "count.job.js", null, 0, Long.MAX_VALUE, null);
        assertEquals("3.0", count);

        count = storage.batchJob(
                "count.job.js", testBase1, 0, Long.MAX_VALUE, null);
        assertEquals("2.0", count);

        count = storage.batchJob(
                "count.job.js", testBase2, 0, Long.MAX_VALUE, null);
        assertEquals("1.0", count);

        count = storage.batchJob(
                "count.job.js", "nosuchbase", 0, Long.MAX_VALUE, null);
        assertEquals("", count);
    }

    public void testCollectIdsByTimestampJob() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        Thread.sleep(100);
        storage.flush(new Record(testId2, testBase2, testContent1));
        long stamp3 = System.currentTimeMillis();
        Thread.sleep(100);
        storage.flush(new Record(testId3, testBase1, testContent1));
        Thread.sleep(100);
        assertBaseCount(testBase1, 2);
        assertBaseCount(testBase2, 1);

        String ids = storage.batchJob(
                "collect_ids.job.js", null, 0, Long.MAX_VALUE, null);
        assertEquals(Strings.join(
                Arrays.asList(testId1, testId2, testId3), "|") + "|", ids);

        ids = storage.batchJob(
                "collect_ids.job.js", null, stamp3, Long.MAX_VALUE, null);
        assertEquals(testId3 + "|", ids);

        ids = storage.batchJob(
                "collect_ids.job.js", null, Long.MAX_VALUE, Long.MAX_VALUE, null);
        assertEquals("", ids);
    }

    public void testDeleteByBaseJob() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        Thread.sleep(100);
        storage.flush(new Record(testId2, testBase2, testContent1));
        Thread.sleep(100);
        storage.flush(new Record(testId3, testBase1, testContent1));
        Thread.sleep(100);
        assertBaseCount(testBase1, 2);
        assertBaseCount(testBase2, 1);

        // Delete all record in testBase1
        String ids = storage.batchJob(
                "delete.job.js", testBase1, 0, Long.MAX_VALUE, null);
        assertEquals(Strings.join(
                Arrays.asList(testId1, testId3), "|") + "|", ids);

        // Assert that testId2 is the only non-deleted record
        // in the entire storage
        QueryOptions nonDeleted = new QueryOptions(false, null, 0, 0);
        ids = storage.batchJob(
                "collect_ids.job.js", null, 0, Long.MAX_VALUE, nonDeleted);
        assertEquals(testId2 + "|", ids);

        // Assert that testId1 and testId3 constitues all deleted records
        // in the entire storage
        QueryOptions deleted = new QueryOptions(true, null, 0, 0);
        ids = storage.batchJob(
                "collect_ids.job.js", null, 0, Long.MAX_VALUE, deleted);
        assertEquals(Strings.join(
                Arrays.asList(testId1, testId3), "|") + "|", ids);
    }

    public void testRenameByBaseAndMtime() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        Thread.sleep(100);
        storage.flush(new Record(testId2, testBase2, testContent1));
        long stamp3 = System.currentTimeMillis();
        Record rec3 = new Record(testId3, testBase1, testContent1);
        Thread.sleep(100);
        storage.flush(rec3);
        Thread.sleep(100);
        assertBaseCount(testBase1, 2);
        assertBaseCount(testBase2, 1);

        // Prepend "foo" to records updated after stamp3 in base testBase1
        // This should be the signle record with testId3
        String ids = storage.batchJob(
                "prepend_foo_id.job.js", testBase1, stamp3, Long.MAX_VALUE, null);
        assertEquals("foo" + testId3 + "|", ids);

        // Assert that testId1 is no longer in the storage
        Record gone = storage.getRecord(testId3, null);
        assertNull(gone);

        // Assert that rec3 has been renamed to "foo" + testId3
        Record newRec3 = storage.getRecord("foo" + rec3.getId(), null);
        rec3.setId("foo" + testId3);
        assertEquals(rec3, newRec3);
    }

    public void assertBaseCount (String base, long expected) throws Exception {
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> iter = new StorageIterator(storage, iterKey);
        long actual = 0;
        while (iter.hasNext()) {
            iter.next();
            actual++;
        }

        if (actual != expected) {
            fail("Base '" + base + "' should contain " + expected
                 + " records, but found " + actual);
        }
    }

}
