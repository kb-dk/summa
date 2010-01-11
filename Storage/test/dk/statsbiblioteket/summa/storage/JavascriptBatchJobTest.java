package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.storage.JavascriptBatchJobTest
 *
 * @author mke
 * @since Jan 7, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class JavascriptBatchJobTest extends StorageTestBase {    

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

    public void testInvalidJobName() throws Exception {
        try {
            storage.batchJob("../../im_in_your_root_eating_your_files.job.js",
                             null, 0, Long.MAX_VALUE, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Success
        }
    }

    public void testUnknownJobName() throws Exception {
        try {
            storage.batchJob("nosuch.job.js",
                             null, 0, Long.MAX_VALUE, null);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException e) {
            // Success
        }
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
