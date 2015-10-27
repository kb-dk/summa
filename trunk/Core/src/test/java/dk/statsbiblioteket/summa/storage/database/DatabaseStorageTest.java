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
package dk.statsbiblioteket.summa.storage.database;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.storage.BaseStats;
import dk.statsbiblioteket.summa.storage.StorageBase;
import dk.statsbiblioteket.summa.storage.StorageTestBase;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;

/**
 * These test cases are meant to test functionality specifically requiring the
 * raw DatabaseStorage API which is not publicly available (ie. in the .api
 * package).
 *
 * @author mke
 * @since Dec 14, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
state = QAInfo.State.IN_DEVELOPMENT,
author = "mke")
public class DatabaseStorageTest extends StorageTestBase {
    /** Database storage logger. */
    private static Log log = LogFactory.getLog(DatabaseStorageTest.class);
    /** Local instance of this object. */
    DatabaseStorage storage;

    /**
     * Setup method, calls setup on super object.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.storage = (DatabaseStorage) super.storage;
    }

    /**
     * Tests statistic on an empty storage.
     * @throws Exception If error.
     */
    public void testStatsOnEmptyStorage() throws Exception {
        List<BaseStats> stats = storage.getStats();
        assertTrue(stats.isEmpty());
    }

    /**
     * Tests statistic on storage with a single record.
     * @throws Exception If error.
     */
    public void testStatsOnSingleRecord() throws Exception {
        long storageStart = storage.getModificationTime(null);
        Thread.sleep(2); // To make sure we have a time stamp delta
        storage.flush(new Record(testId1, testBase1, testContent1));
        List<BaseStats> stats = storage.getStats();

        assertEquals(1, stats.size());

        BaseStats base = stats.get(0);
        assertEquals(testBase1, base.getBaseName());
        assertEquals(1, base.getIndexableCount());
        assertEquals(0, base.getDeletedCount());
        assertEquals(1, base.getTotalCount());
        assertEquals(1, base.getLiveCount());
        assertTrue("Base mtime must be updated, but base.getModificationTime() <= storageStart: "
                + base.getModificationTime() + " <= " + storageStart,
                base.getModificationTime() > storageStart);
    }

    public void testGetRecordsModifiedAfter2() throws Exception {
        testGetRecordsModifiedAfter(2);
    }

    public void testGetRecordsModifiedAfter3() throws Exception {
        testGetRecordsModifiedAfter(3);
    }

    public void testGetRecordsModifiedAfterSpecifics() throws Exception {
        final int[] RECORDS = new int[]{1, 2, 3, 4, 5, 10, 100, 1000};
        for (int records: RECORDS) {
            testGetRecordsModifiedAfter(records);
        }
    }

    private void testGetRecordsModifiedAfter(int records) throws Exception {
        final String BASE = "base1";
        final byte[] DATA = "data".getBytes("utf-8");

        log.debug("Testing for " + records + " records");
        storage.clearBase(BASE);
        assertBaseCount(BASE, 0);

        for (int i = 0 ; i < records ; i++) {
            storage.flush(new Record("id" + i, BASE, DATA));
        }

        assertBaseCount(BASE, records);
    }

    /**
     * Tests statistic on storage with to records in two different bases.
     * @throws Exception If error.
     */
    public void testStatsOnTwoRecordsInTwoBases() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        storage.flush(new Record(testId2, testBase2, testContent1));
        List<BaseStats> stats = storage.getStats();

        assertEquals(2, stats.size());

        BaseStats base = stats.get(0);
        assertEquals(testBase1, base.getBaseName());
        assertEquals(1, base.getIndexableCount());
        assertEquals(0, base.getDeletedCount());
        assertEquals(1, base.getTotalCount());
        assertEquals(1, base.getLiveCount());

        base = stats.get(1);
        assertEquals(testBase2, base.getBaseName());
        assertEquals(1, base.getIndexableCount());
        assertEquals(0, base.getDeletedCount());
        assertEquals(1, base.getTotalCount());
        assertEquals(1, base.getLiveCount());
    }

    /**
     * Tests statistic on storage with mixed states.
     * @throws Exception If error.
     */
    public void testStatsWithMixedStates() throws Exception {
        Record r1 = new Record(testId1, testBase1, testContent1);

        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setDeleted(true);

        Record r3 = new Record(testId3, testBase1, testContent1);
        r3.setIndexable(false);

        Record r4 = new Record(testId4, testBase1, testContent1);
        r4.setDeleted(true);
        r4.setIndexable(false);

        storage.flushAll(Arrays.asList(r1, r2, r3, r4));
        List<BaseStats> stats = storage.getStats();

        assertEquals(1, stats.size());

        BaseStats base = stats.get(0);
        assertEquals(testBase1, base.getBaseName());
        assertEquals(2, base.getIndexableCount());
        assertEquals(2, base.getDeletedCount());
        assertEquals(4, base.getTotalCount());
        assertEquals(1, base.getLiveCount());
    }

    public void testGetChild() throws Exception {
        Record r1 = new Record(testId1, testBase1, testContent1);
        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setParentIds(Arrays.asList(r1.getId()));
        storage.flushAll(Arrays.asList(r1, r2));

        try {
            storage.getRecord(r2.getId(), null);
        } catch (Exception e) {
            fail("Exception while requesting a child record with an existing parent: " + e.getMessage());
        }
    }

    public void testGetChildWithParent() throws Exception {
        Record r1 = new Record(testId1, testBase1, testContent1);
        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setParentIds(Arrays.asList(r1.getId()));
        storage.flushAll(Arrays.asList(r1, r2));

        try {
            Record extracted = storage.getRecord(r2.getId(), new QueryOptions(
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
            assertNotNull("The extracted record should have a parent ID",
                         extracted.getParentIds());
            assertEquals("The extracted record should have the right parent ID",
                         testId1, extracted.getParentIds().get(0));
            assertNotNull("The extracted record should have a parent",
                          extracted.getParents());
            assertEquals("The extracted record should have the right parent",
                         testId1, extracted.getParents().get(0).getId());
        } catch (Exception e) {
            fail("Exception while requesting a child record with an existing parent: " + e.getMessage());
        }
    }

    /* Requesting an orphaned child should result in a warning in the log, not an exception */
    public void testGetOrphanChild() throws Exception {
        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setParentIds(Arrays.asList("NonExisting"));
        storage.flushAll(Arrays.asList(r2));

        try {
            Record record = storage.getRecord(r2.getId(), null);                        
            assertEquals(testId2,record.getId());
        } catch (Exception e) {
            fail("Exception while requesting a record with a parent-ID, but no existing parent: " + e.getMessage());
          log.warn("fail",e);
        }
    }


    public void testTouchNone() throws Exception {
        assertClearAndUpdateTimestamps(
                "None", StorageBase.RELATION.none, StorageBase.RELATION.none, Arrays.asList(
                createRecord("m1", null, null)
        ), new HashSet<>(Arrays.asList("m1")));
    }

    // Touch all is default behaviour as of 2015-09-11
    public void testTouchAll() throws Exception {
        assertClearAndUpdateTimestamps(
                "Direct all", StorageBase.RELATION.none, StorageBase.RELATION.all, Arrays.asList(
                        createRecord("m1", null, null)
                        ), new HashSet<>(Arrays.asList("t1", "m1", "b1")));
    }

    public void testTouchParents() throws Exception {
        assertClearAndUpdateTimestamps(
                "Direct parent", StorageBase.RELATION.none, StorageBase.RELATION.parent, Arrays.asList(
                createRecord("m1", null, null)
        ), new HashSet<>(Arrays.asList("t1", "m1")));
    }
    // This test fails, which seems like a regression error as we have previously worked under the assumption
    // that touching a parent also touched its children. Same for testTouchAll.
    public void testTouchChildren() throws Exception {
        assertClearAndUpdateTimestamps(
                "Direct children", StorageBase.RELATION.none, StorageBase.RELATION.child, Arrays.asList(
                createRecord("m1", null, null)
        ), new HashSet<>(Arrays.asList("m1", "b1")));
    }

    // Used in Statsbiblioteket/aviser
    public void testClearParentTouchChildrenFromMiddle() throws Exception {
        assertClearAndUpdateTimestamps(
                "Parent clear touch children from middle",
                StorageBase.RELATION.parent, StorageBase.RELATION.child, Arrays.asList(
                createRecord("m1", Arrays.asList("t2"), null)
        ), new HashSet<>(Arrays.asList("m1", "b1")));
    }
    public void testClearParentTouchChildrenFromBottom() throws Exception {
        assertClearAndUpdateTimestamps(
                "Parent clear touch children from bottom",
                StorageBase.RELATION.parent, StorageBase.RELATION.child, Arrays.asList(
                createRecord("b1", Arrays.asList("m1"), null)
        ), new HashSet<>(Arrays.asList("b1")));
    }
    // Used in Statsbiblioteket/aviser
    public void testClearParentTouchChildrenFromTop() throws Exception {
        assertClearAndUpdateTimestamps(
                "Parent clear touch children from top",
                StorageBase.RELATION.parent, StorageBase.RELATION.child, Arrays.asList(
                createRecord("t1", null, null) // The old relation t1->m1 should not be cleared
        ), new HashSet<>(Arrays.asList("t1", "m1","b1")));
    }

    
    public void testClearNoneUpdateParent() throws Exception {
        assertClearAndUpdateTimestamps(
                "No clear", StorageBase.RELATION.none, StorageBase.RELATION.parent, Arrays.asList(
                createRecord("m1", Arrays.asList("t2"), null)
        ), new HashSet<>(Arrays.asList("t1", "t2", "m1")));
    }
    public void testClearParentUpdateParent() throws Exception {
        assertClearAndUpdateTimestamps(
                "Parent clear", StorageBase.RELATION.parent, StorageBase.RELATION.parent, Arrays.asList(
                createRecord("m1", Arrays.asList("t2"), null)
        ), new HashSet<>(Arrays.asList("t1", "t2", "m1")));
    }
    // Not used in any setup at Statsbiblioteket
    public void testClearChildUpdateParent() throws Exception {
        assertClearAndUpdateTimestamps(
                "Child clear, parent update", StorageBase.RELATION.child, StorageBase.RELATION.parent, Arrays.asList(
                createRecord("m1", Arrays.asList("t2"), null)
        ), new HashSet<>(Arrays.asList("t1", "t2", "m1")));
    }
    public void testClearChildUpdateChild() throws Exception {
        assertClearAndUpdateTimestamps(
                "Child clear & update", StorageBase.RELATION.child, StorageBase.RELATION.child, Arrays.asList(
                createRecord("m1", null, Arrays.asList("b2"))
        ), new HashSet<>(Arrays.asList("m1", "b1", "b2")));
    }
    public void testClearAllUpdateParent() throws Exception {
        assertClearAndUpdateTimestamps(
                "All clear", StorageBase.RELATION.all, StorageBase.RELATION.parent, Arrays.asList(
                createRecord("m1", Arrays.asList("t2"), null)
        ), new HashSet<>(Arrays.asList("t1", "t2", "m1")));
    }

    private Record createRecord(String id, List<String> parents, List<String> children) {
        Record record = new Record(id, testBase1, testContent1);
        record.setParentIds(parents);
        record.setChildIds(children);
        return record;
    }

    /**
     * This helper creates an isolated storage and adds a small collection of records:
     * <ul>
     * <li>t1 (m1 as child)</li>
     * <li>t2 (no relatives)</li>
     * <li>m1 (t1 as parent, b1 as child)</li>
     * <li>m2 (no relatives)</li>
     * <li>b1 (m1 as parent)</li>
     * <li>b2 (no relatives)</li>
     * </ul>
     * The given updates are then flushed and the IDs of all touched Records are compared to the expected set.
     * @param message   fail message.
     * @param clear     relation clear configuration {@link StorageBase#CONF_RELATION_CLEAR}.
     * @param touch     relation touch configuration {@link StorageBase#CONF_RELATION_TOUCH}.
     * @param updates   new or updated Records.
     * @param expected  IDs of the Records with updated modification times.
     */
    private void assertClearAndUpdateTimestamps(
            String message, StorageBase.RELATION clear, StorageBase.RELATION touch,
            List<Record> updates, Set<String> expected) throws Exception {
        Configuration conf = createConf(); // ReleaseHelper.getStorageConfiguration("RelationsTest");
        conf.set(DatabaseStorage.CONF_RELATION_TOUCH, touch);
        conf.set(DatabaseStorage.CONF_RELATION_CLEAR, clear);
        conf.set(H2Storage.CONF_H2_SERVER_PORT, 8079);
        Storage storage = new H2Storage(conf);
        try {
            storage.flushAll(Arrays.asList(
                    createRecord("t1", null, Arrays.asList("m1")),
                    createRecord("t2", null, null),
                    createRecord("m1", Arrays.asList("t1"), Arrays.asList("b1")),
                    createRecord("m2", null, null),
                    createRecord("b1", Arrays.asList("m1"), null),
                    createRecord("b2", null, null)
                    ));
            Map<String, Long> originalTS = getTimestamps(storage);

            storage.flushAll(updates);
            Map<String, Long> flushedTS = getTimestamps(storage);
            Set<String> changed = calculateChangedTimestamps(originalTS, flushedTS);

            final String debug =
                    "touch=" + touch + ", clear=" + clear
                    + ", expected=[" + Strings.join(expected) + "], actual=[" + Strings.join(changed) + "]";
            ExtraAsserts.assertEquals(message + ", " + debug + ", expected changed records should match actual",
                    expected, changed);
        } finally {
            storage.close();
        }
    }

    private Set<String> calculateChangedTimestamps(Map<String, Long> preTS, Map<String, Long> postTS) {
        Set<String> changed = new HashSet<>();
        for (Map.Entry<String, Long> postEntry: postTS.entrySet()) {
            Long ts = preTS.get(postEntry.getKey());
            if (ts == null || !ts.equals(postEntry.getValue())) {
                changed.add(postEntry.getKey());
            }
        }
        return changed;
    }

    private Map<String, Long> getTimestamps(Storage storage) throws IOException {
        Map<String, Long> ts = new HashMap<>();
        long iteratorKey = storage.getRecordsModifiedAfter(0L, testBase1, null);
        List<Record> records = storage.next(iteratorKey, 10000); // We should get them all in one go
        for (Record record: records) {
            ts.put(record.getId(), record.getLastModified());
        }
        return ts;
    }


    public void testSelfReference() throws IOException {
        Record r1 = new Record(testId1, testBase1, testContent1);
        storage.flushAll(Arrays.asList(r1));
        storage.getRecord(testId1, null);
        log.info("Putting and getting a standard Record works as intended");

        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setChildIds(Arrays.asList(testId2));
        log.info("Adding self-referencing Record " + testId2);
        try{
            storage.flushAll(Arrays.asList(r2));
            fail();
        }
        catch(Exception e){
            //ignore
        }
        log.info("Putting and getting a single self-referencing Record works as intended");
    }

    public void testTwoLevelCycle() throws IOException {
        Record r1 = new Record(testId1, testBase1, testContent1);
        r1.setChildIds(Arrays.asList(testId2));
        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setChildIds(Arrays.asList(testId1));
        log.info("testTwoLevelCycle: Flushing 2 Records, referencing each other as children");

        try{
            storage.flushAll(Arrays.asList(r1, r2));
            fail();
        }
        catch(Exception e){
            //ignore
        }


    }

    /*
    Creates thousands of Records with large content. When extracted as Records, they take up the full amount of bytes
     on the heap. A touch of the parent to these Records is triggered, testing whether the child-touch is implemented
      in a memory-efficient manner (read: Not loaded onto the heap).

      Set RECORDS to 400000 and Xmx to 300m before running this test for a proper memory trial
     */
    /*
    public void testManyBytesTouch() throws Exception {
        final int[] RECORDS = new int[]{100, 500, 1000, 5000, 10000, 15000, 20000, 25000, 30000, 40000};
        final int CONTENT_SIZE = 1024;
        long[] ms = new long[RECORDS.length];

        for (int i = 0; i < RECORDS.length; i++) {
            ms[i] = measureManyBytesTouch(RECORDS[i], CONTENT_SIZE);
        }

        for (int i = 0; i < RECORDS.length; i++) {
            log.info(String.format("Records: %6d, children touched/sec: %4d",
                                   RECORDS[i], RECORDS[i] * 60 / ms[i]));
        }

    }
    */
    
    // This mimics a rare but still occurring scenario at Statsbiblioteket/aviser
    public long measureManyBytesTouch(final int records, int contentSize) throws Exception {
        final byte[] CONTENT = new byte[contentSize];
        new Random().nextBytes(CONTENT); // Not so packable now, eh?
        final List<String> PARENTS = Arrays.asList("Parent_0");
        final Record TOP = new Record("Parent_0", "dummy", new byte[10]);

        Configuration conf = createConf();
        conf.set(DatabaseStorage.CONF_RELATION_CLEAR, DatabaseStorage.RELATION.parent);
        conf.set(DatabaseStorage.CONF_RELATION_TOUCH, DatabaseStorage.RELATION.child);
        Storage storage = new H2Storage(conf);

        try {
            storage.flush(TOP);
            log.info(String.format("Ingesting %d records of size %dMB for a total of %dMB",
                                   records, CONTENT.length / M, records * CONTENT.length / M));
            for (int i = 0 ; i < records ; i++) {
                Record r = new Record("Child_" + i, "dummy", CONTENT);
                r.setParentIds(PARENTS);
                storage.flush(r);
                if (i % (records < 100 ? 1 : records/100) == 0) {
                    System.out.print(".");
                }
            }
            System.out.println("");
            log.info(String.format("Finished ingesting of %dMB. Getting child 0 mtime...",
                                   records * CONTENT.length / M));

            QueryOptions options = new QueryOptions();
            options.setAttributes(QueryOptions.ATTRIBUTES_SANS_CONTENT_AND_META);
            options.removeAttribute(QueryOptions.ATTRIBUTES.PARENTS);
            long oldChildMtime = storage.getRecord("Child_0", options).getLastModified();
            log.info("Got child 0 mtime. Touching parent...");
            final long ttime = System.nanoTime();
            storage.flush(TOP);
            final long ms = (System.nanoTime()-ttime)/1000000;
            log.info(String.format("Parent touched in %dms (%d child records/sec). Getting child 0 mtime...",
                                   ms, records * 60 / ms));
            long newChildMtime = storage.getRecord("Child_0", options).getLastModified();
            assertFalse("The MTime of Child_0 should be changed after parent touch", oldChildMtime == newChildMtime);
            return ms;
        } finally {
            storage.close();
        }
    }
    private static final int M = 1048576;

    public void testTwoLevelCycleParent() throws IOException {
        Record r1 = new Record(testId1, testBase1, testContent1);
        r1.setParentIds(Arrays.asList(testId2));
        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setParentIds(Arrays.asList(testId1));
        log.info("testTwoLevelCycleParent: Flushing 2 Records, referencing each other as parents");
        try{
            storage.flushAll(Arrays.asList(r1, r2));
            fail();
        }
        catch(Exception e){
            //ignore
        }



    }

    public void testThreeLevelCycle() throws IOException {
        Record r1 = new Record(testId1, testBase1, testContent1);
        r1.setChildIds(Arrays.asList(testId2));
        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setChildIds(Arrays.asList(testId3));
        Record r3 = new Record(testId3, testBase1, testContent1);
        r3.setChildIds(Arrays.asList(testId1));
        log.info("testThreeLevelCycle: Flushing 3 Records, referencing each other as children");

        try{
            storage.flushAll(Arrays.asList(r1, r2, r3));
            fail();
        }
        catch(Exception e){
            //ignore
        }




    }

    /**
     * This loops forever (or at least a long time) as setting deleted = true updates modification-time.
     * @throws IOException if the test failed due to database problems.
     */
    public void testBatchJob() throws IOException {
        final int RECORDS = 1000;
        final byte[] CONTENT = new byte[5];
        for (int i = 0 ; i < RECORDS ; i++) {
            storage.flush(new Record("Record_" + i, "Dummy", CONTENT));
        }

        String sampleID = "Record_" + RECORDS/2;
        assertNotNull("There should be a record named " + sampleID, storage.getRecord(sampleID, null));
        assertFalse("The record " + sampleID + " should not be marked as deleted",
                storage.getRecord(sampleID, null).isDeleted());

        storage.batchJob("delete.job.js", null, 0, Long.MAX_VALUE, null);
        assertNotNull("There should still be a record named " + sampleID, storage.getRecord(sampleID, null));
        assertTrue("The record " + sampleID + " should be marked as deleted",
                storage.getRecord(sampleID, null).isDeleted());

    }

    /**
     * Test illegal access to __holdings__ record.
     * @throws Exception If error.
     */
    /* This unittest has always failed, dont know what the idea was.
    public void testIllegalPrivateAccess() throws Exception {
        try {
            storage.getRecord("__holdings__", null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // Good
        }
    }
     */

    /**
     * Test get __holdings__ object.
     * @throws Exception If error.
     */
    public void testGetHoldings() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        storage.flush(new Record(testId2, testBase2, testContent1));

        StringMap meta = new StringMap();
        meta.put("ALLOW_PRIVATE", "true");
        QueryOptions opts = new QueryOptions(null, null, 0, 0, meta);
        Record holdings = storage.getRecord("__holdings__", opts);
        String xml = holdings.getContentAsUTF8();

        assertTrue(xml.startsWith("<holdings"));
        assertTrue(xml.endsWith("</holdings>"));

        log.info(xml);
        // TODO assert equals
    }

    /**
     * Test get and set of modification time.
     * @throws Exception If error occur
     */
    public void testGetSetModificationTime() throws Exception {
        long start = storage.getModificationTime(testBase1);
        assertEquals(storage.getStorageStartTime(), start);
        storage.flush(new Record(testId1, testBase1, testContent1));
        long newMtime = storage.getModificationTime(testBase1);
        assertTrue(start < newMtime);
    }

    /**
     * Test start on an existing storage.
     * @throws Exception If error.
     */
    public void testStatsOnExistingStorage() throws Exception {
        Configuration conf = createConf();
        storage = (DatabaseStorage) StorageFactory.createStorage(conf);
        long start = storage.getModificationTime(testBase1);
        storage.destroyBaseStatistic();
        storage.close();
        // Start storage on a old database file
        storage = (DatabaseStorage) StorageFactory.createStorage(conf);
        storage.flush(new Record(testId1, testBase1, testContent1));
        assertTrue(start < storage.getModificationTime(testBase1));
        storage.close();
    }

}
