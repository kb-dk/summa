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
import dk.statsbiblioteket.summa.storage.api.filter.RecordWriter;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.database.postgresql.PostGreSQLStorageTest;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public void testLocksLocalH2Storage() throws Exception {
        testLocks(storage);
    }

    public void testLocksRemotePostgreSQLMars() throws Exception {
        DatabaseStorage storage = PostGreSQLStorageTest.getDeveloperTestStorage(PostGreSQLStorageTest.MARS_AVISER, false);
        assertNotNull(storage);
        testLocks(storage);
    }

    public void testLocksRemotePostgreSQLMars2() throws Exception {
        DatabaseStorage storage = PostGreSQLStorageTest.getDeveloperTestStorage(PostGreSQLStorageTest.MARS_AVISER, false);
        assertNotNull(storage);
        log.info("Attempting call to updateBaseMTimeAndStats");
        storage.updateBaseMTimeAndStats("dummy"); // Not normally called from outside, but triggers lock problem
        // Does not get any further with PostgreSQL against mars
    }

    /**
     * Connects to a Storage, ingests Records, requests stats, then attempts to clear the ingested Records.
     */
    private void testLocks(DatabaseStorage storage) throws Exception {
        storage.flush(new Record("dummy1", "dummy", new byte[0]));
        storage.flush(new Record("dummy2", "dummy", new byte[0]));
        assertNotNull("getStats() should give a result", storage.getStats());
        storage.clearBase("dummy");
        storage.flush(new Record("dummy3", "dummy", new byte[0]));
    }

    public void testParentChildClearBase() throws Exception {
        // We need intermediates to get paging
        DatabaseStorage storage = createStorageWithParentChild(DatabaseStorage.DEFAULT_PAGE_SIZE + 10);
        try {
            assertFalse("The record Parent should not be marked as deleted",
                        storage.getRecord("Parent", null).isDeleted());
            assertFalse("The record Child1 should not be marked as deleted",
                        storage.getRecord("Child1", null).isDeleted());
            assertFalse("The record Child2 should not be marked as deleted",
                        storage.getRecord("Child2", null).isDeleted());

            storage.clearBase("dummy");

            assertTrue("After clear the record Parent should be marked as deleted",
                        storage.getRecord("Parent", null).isDeleted());
            assertTrue("After clear the record Child1 should be marked as deleted",
                        storage.getRecord("Child1", null).isDeleted());
            assertTrue("After clear the record Child2 should be marked as deleted",
                        storage.getRecord("Child2", null).isDeleted());
        } finally {
            storage.close();
        }
    }

    public void testRelationHiding() throws Exception {
        Configuration conf = createConf();
        conf.set(DatabaseStorage.CONF_EXPAND_RELATIVES_ID_LIST, true);
        conf.set(H2Storage.CONF_H2_SERVER_PORT, 8079 + storageCounter++);
        conf.set(DatabaseStorage.CONF_BASES_WITH_STORED_RELATIONS, new ArrayList<>(Collections.singleton("dummy")));
        DatabaseStorage storage = new H2Storage(conf);
        try {
            { // IDs should not be assigned from relations
                Record r1 = new Record("Ra1", "dummy", new byte[0]);
                r1.setChildIds(Collections.singletonList("Ra2"));
                storage.flush(r1);

                Record r2 = new Record("Ra2", "dummy", new byte[0]);
                storage.flush(r2);

                Record rec = storage.getRecord("Ra1", null);
                assertNotNull("There should be a record 'Ra1' before update", rec);
                assertTrue("Ra1 should have child IDs",
                           rec.getChildIds() != null && !rec.getChildIds().isEmpty());

                QueryOptions options = new QueryOptions();
                options.setAttributes(new QueryOptions.ATTRIBUTES[]{
                        QueryOptions.ATTRIBUTES.ID,
                        QueryOptions.ATTRIBUTES.BASE});
                Record rec2 = storage.getRecord("Ra1", options);
                assertNotNull("There should be a record 'Ra1' when requesting only ID & BASE", rec2);
                assertTrue("Ra1 should have child IDs with limited meta",
                           rec.getChildIds() != null && !rec.getChildIds().isEmpty());
            }

            { // Cut named connection
                Record r1 = new Record("Ra1", "dummy", new byte[0]);
                storage.flush(r1);

                Record rec = storage.getRecord("Ra1", null);
                assertNotNull("There should be a record 'Ra1' after update", rec);
                assertTrue("Ra1 should have no child IDs but has " +
                           (rec.getChildIds() == null ? 0 : rec.getChildIds().size()),
                           rec.getChildIds() == null || rec.getChildIds().isEmpty());
            }

            { // Multiple IDs should be assignable
                Record r1 = new Record("Ra1", "dummy", new byte[0]);
                r1.setChildIds(Arrays.asList("Ra2", "Ra3"));
                storage.flush(r1);

                Record r2 = new Record("Ra2", "dummy", new byte[0]);
                storage.flush(r2);

                Record r3 = new Record("Ra3", "dummy", new byte[0]);
                storage.flush(r3);

                Record rec = storage.getRecord("Ra1", null);
                assertNotNull("There should be a record 'Ra1' after multi-child update", rec);
                assertEquals("Ra1 should have the right amount of child-IDs",
                             2, rec.getChildIds().size());
                assertEquals("Ra1 should have the right amount of children",
                             2, rec.getChildren().size());

            }
        } finally {
            storage.close();
        }
    }

    public void testRelationReset() throws Exception {
        Configuration conf = createConf();
        conf.set(DatabaseStorage.CONF_EXPAND_RELATIVES_ID_LIST, true);
        conf.set(H2Storage.CONF_H2_SERVER_PORT, 8079+storageCounter++);
        DatabaseStorage storage = new H2Storage(conf);
        try {
            // R1 hasChild R2
            {
                Record r1 = new Record("R1", "dummy", new byte[0]);
                r1.setChildIds(Collections.singletonList("R2"));
                storage.flush(r1);

                Record r2 = new Record("R2", "dummy", new byte[0]);
                storage.flush(r2);

                Record rec = storage.getRecord("R1", null);
                assertTrue("R1-hasChild-R2", rec.getChildren() != null && rec.getChildren().size() == 1);
            }

            // Cyclic
            {
                Record r2 = new Record("R2", "dummy", new byte[0]);
                r2.setChildIds(Collections.singletonList("R1"));
                storage.flush(r2);

                Record rec1 = storage.getRecord("R1", null);
                assertTrue("Cyclic R1 children", rec1.getChildren() != null && rec1.getChildren().size() == 1);
                Record rec2 = storage.getRecord("R2", null);
                assertTrue("Cyclic R2 children", rec2.getChildren() != null && rec2.getChildren().size() == 1);
            }

            // Cycle broken
            {
                Record r1 = new Record("R1", "dummy", new byte[0]);
                r1.setChildIds(Collections.<String>emptyList());
                storage.flush(r1);

                Record rec1 = storage.getRecord("R1", null);
                assertTrue("Cycle broken R1 children", rec1.getChildren() == null || rec1.getChildren().isEmpty());
                Record rec2 = storage.getRecord("R2", null);
                assertTrue("Cycle broken R2 children", rec2.getChildren() != null && rec2.getChildren().size() == 1);
            }

        } finally {
            storage.close();
        }
    }

    // For performance reasons the authoritative Records in the Statsbiblioteket aviser project should
    // not have their childrenIDs enriched from the relations table
    public void testRelativesExpansion() throws Exception {
        DatabaseStorage storage = createStorageWithParentChild();
        try {
            List<Record> extracted = getRecordsWithParents(storage, "dummy");
            assertEquals("There should be the right number of Records in the database", 3, extracted.size());
            for (Record record: extracted) {
                if ("Child1".equals(record.getId()) || "Child2".equals(record.getId())) {
//                    assertEquals("The parentIDs for " + record.getId() + " should have the right number of entries",
//                                 1, record.getParentIds().size());
//                    assertEquals("The parentID for " + record.getId() + " should be as expected",
//                                 "Parent", record.getParentIds().get(0));
                    assertNotNull("There should be a parent Record for " + record.getId(), record.getParents());
                    assertEquals("There should be the right number of parent Records for " + record.getId(),
                                 1, record.getParents().size());
                } else if ("Parent".equals(record.getId())) {
                    // This is the important part: The childIDs are not stored directly in the parent, but are
                    // extracted from the relations table. We don't need them in the aviser project and extracting
                    // them takes 2-300 ms, which is done each time a Record is resolved.
                    // conf.set(DatabaseStorage.CONF_EXPAND_RELATIVES_ID_LIST, false);
                    // in the Storage setup signals that this expansion should not take place.
                    // The relevant part in DatabaseStorage seems to be DatabaseStorage#scanRecord
                    assertNull("The childIDs for " + record.getId() + " should be empty but was "
                               + (record.getChildIds() == null ? "N/A" : record.getChildIds().size()),
                               record.getChildIds());
                } else {
                    fail("Unexpected record " + record.getId());
                }
            }
        } finally {
            storage.close();
            Thread.sleep(200); // Wait for freeing of resources
        }
    }

    public void testRelativesGetOnlyParentExpansion() throws Exception {
        DatabaseStorage storage = createStorageWithParentChild();
        try {
            QueryOptions parentOnly = new QueryOptions(false, false, 0, 1);
            parentOnly.setAttributes(QueryOptions.ATTRIBUTES_ALL);
            parentOnly.removeAttribute(QueryOptions.ATTRIBUTES.CHILDREN);
            {

                Record child = storage.getRecord("Child1", parentOnly);
                assertTrue("Sans-children record should have a parent",
                           child.getParents() != null && child.getParents().size() == 1);

                Record parent = child.getParents().get(0);
                assertNull("Sans-children record should have a parent without children IDs", child.getChildIds());
                assertNull("Sans-children record should have a parent without children", child.getChildren());
            }
            {
                Record parent = storage.getRecord("Parent", parentOnly);
                assertNull("Sans-children parent should have no children", parent.getChildren());
                assertNull("Sans-children parent should have no children IDs", parent.getChildIds());
            }

            {
                Record parent = storage.getRecord("Parent", null);
                assertNotNull("Base parent should have children", parent.getChildren());
                assertEquals("Base parent should have the right number of children", 2, parent.getChildren().size());
            }
            // Why doesn't the expansion below work?
/*            {
                Record child = storage.getRecord("Child1", null);
                assertTrue("Base record should have a parent",
                           child.getParents() != null && child.getParents().size() == 1);

                Record parent = child.getParents().get(0);
                assertNotNull("Sans-children record should have a parent with children IDs", child.getChildIds());
                assertNotNull("Base record should have a parent with children", child.getChildren());
                assertEquals("Base record should have a parent with 2 children", 2, child.getChildren().size());
            }*/
        } finally {
            storage.close();
        }
    }

    /*
    When requesting parent expansion, we also want Records without parents.
     */
    public void testAllRecordsExpandOnlyParent() throws Exception {
        DatabaseStorage storage = getStorageWithMixedRelations(false);

        List<Record> extracted;
        try {
            extracted = getRecordsWithParents(storage, "aviser");
        } finally {
            storage.close();
        }
        assertMixedRelations("Generic", extracted);
    }
    public void testAllRecordsExpandOnlyParentNullBase() throws Exception {
        DatabaseStorage storage = getStorageWithMixedRelations(false);

        List<Record> extracted;
        try {
            extracted = getRecordsWithParents(storage, null);
        } finally {
            storage.close();
        }
        assertMixedRelations("Generic", extracted);
    }

    public void testAllRecordsExpandImplicitOptimize() throws Exception {
        DatabaseStorage storage = getStorageWithMixedRelations(true);

        List<Record> extracted;
        try {
            extracted = getRecordsWithParents(storage, "aviser");
        } finally {
            storage.close();
        }
        assertMixedRelations("Implicit optimize", extracted);
    }

    public void testAllRecordsExpandOnlyParentOptimized() throws Exception {
        DatabaseStorage storage = getStorageWithMixedRelations(false);

        List<Record> extracted;
        try {
            extracted = storage.getRecordsModifiedAfterOptimized(
                    0L, "aviser", null, DatabaseStorage.OPTIMIZATION.singleParent).getKey();
        } finally {
            storage.close();
        }
        assertMixedRelations("Optimized", extracted);
    }

    public void testAllRecordsExpandOnlyParentOptimizedNullBase() throws Exception {
        DatabaseStorage storage = getStorageWithMixedRelations(false);

        List<Record> extracted;
        try {
            extracted = storage.getRecordsModifiedAfterOptimized(
                    0L, null, null, DatabaseStorage.OPTIMIZATION.singleParent).getKey();
        } finally {
            storage.close();
        }
        assertMixedRelations("Optimized", extracted);
    }

    private void assertMixedRelations(String designation, List<Record> extracted) {
        assertEquals("All Records should be extracted", 3, extracted.size());
        boolean foundChild = false;
        for (Record record: extracted) {
            if ("ChildWithParent".equals(record.getId())) {
                assertNotNull(designation + ". Child record should have a parent list", record.getParents());
                assertEquals(designation + ". Child record should have 1 parent in the parent list",
                             1, record.getParents().size());
                foundChild = true;
                assertTrue("The parent should be marked as deleted", record.getParents().get(0).isDeleted());
            }
        }
        if (!foundChild) {
            fail("Unable to locate Record ChildWithParent");
        }
    }
    private DatabaseStorage getStorageWithMixedRelations(boolean optimize) throws Exception {
        Record parent1 = new Record("ParentWithChild", "aviser", new byte[0]);
        parent1.setDeleted(true);
        Record child1 = new Record("ChildWithParent", "aviser", new byte[0]);
        child1.setParentIds(Collections.singletonList(parent1.getId()));

        Record neutral1 = new Record("RecordWithoutFamily", "aviser", new byte[0]);

        int storagePort = 8079+storageCounter++;

        Configuration conf = createConf();
        conf.set(DatabaseStorage.CONF_RELATION_TOUCH, DatabaseStorage.RELATION.child);
        conf.set(DatabaseStorage.CONF_RELATION_CLEAR, DatabaseStorage.RELATION.parent);
        conf.set(QueryOptions.CONF_CHILD_DEPTH, 0);
        conf.set(QueryOptions.CONF_PARENT_DEPTH, 1);
        conf.set(QueryOptions.CONF_ATTRIBUTES, "all");
        conf.set(QueryOptions.CONF_FILTER_INDEXABLE, "null");
        conf.set(QueryOptions.CONF_FILTER_DELETED, "null");
        conf.set(DatabaseStorage.CONF_USE_OPTIMIZATIONS, optimize);

        conf.set(H2Storage.CONF_H2_SERVER_PORT, storagePort);
        DatabaseStorage storage = new H2Storage(conf);
        storage.flushAll(Arrays.asList(parent1, child1, neutral1));
        return storage;
    }

    public void testRelativesGetOnlyParentExpansionDefault() throws Exception {
        Record parent1 = new Record("Parent", "dummy", new byte[0]);

        Record child1 = new Record("Child1", "dummy", new byte[0]);
        child1.setParentIds(Collections.singletonList("Parent"));

        Record child2 = new Record("Child2", "dummy", new byte[0]);
        child2.setParentIds(Collections.singletonList("Parent"));

        Configuration conf = createConf();
        conf.set(DatabaseStorage.CONF_RELATION_TOUCH, DatabaseStorage.RELATION.child);
        conf.set(DatabaseStorage.CONF_RELATION_CLEAR, DatabaseStorage.RELATION.parent);
        conf.set(QueryOptions.CONF_CHILD_DEPTH, 0);
        conf.set(QueryOptions.CONF_PARENT_DEPTH, 3);
        conf.set(QueryOptions.CONF_ATTRIBUTES, "all");
        conf.set(QueryOptions.CONF_FILTER_INDEXABLE, "null");
        conf.set(QueryOptions.CONF_FILTER_DELETED, "null");

        // We only want the IDs stored directly in the Record
        conf.set(DatabaseStorage.CONF_EXPAND_RELATIVES_ID_LIST, false);
        conf.set(H2Storage.CONF_H2_SERVER_PORT, 8079+storageCounter++);
        DatabaseStorage storage = new H2Storage(conf);
        try {
            storage.flushAll(Arrays.asList(parent1, child1, child2));

            {

                Record child = storage.getRecord("Child1", null);
                assertTrue("Default QueryOptions record should have a parent",
                           child.getParents() != null && child.getParents().size() == 1);

                Record parent = child.getParents().get(0);
                assertNull("Default QueryOptions record should have a parent without children IDs", child.getChildIds());
                assertNull("Default QueryOptions record should have a parent without children", child.getChildren());
            }
            {
                Record parent = storage.getRecord("Parent", null);
                assertNull("Default QueryOptions parent should have no children", parent.getChildren());
                assertNull("Default QueryOptions parent should have no children IDs", parent.getChildIds());
            }

            {
                QueryOptions withChildren = new QueryOptions(null, null, -1, -1); // Why does this not work when depths are 3 and 3?
                withChildren.setAttributes(QueryOptions.ATTRIBUTES_ALL);
                withChildren.removeAttribute(QueryOptions.ATTRIBUTES.META);
                Record parent = storage.getRecord("Parent", withChildren);
                assertNotNull("With children parent should have children", parent.getChildren());
                assertEquals("With children parent should have the right number of children", 2, parent.getChildren().size());
            }
            // Why doesn't the expansion below work?
/*            {
                Record child = storage.getRecord("Child1", null);
                assertTrue("Base record should have a parent",
                           child.getParents() != null && child.getParents().size() == 1);

                Record parent = child.getParents().get(0);
                assertNotNull("Sans-children record should have a parent with children IDs", child.getChildIds());
                assertNotNull("Base record should have a parent with children", child.getChildren());
                assertEquals("Base record should have a parent with 2 children", 2, child.getChildren().size());
            }*/
        } finally {
            storage.close();
        }
    }

    public void testRelativesSimple() throws Exception {
        DatabaseStorage storage = createStorageWithParentChild();
        try {
            Record fullTree = storage.getRecordWithFullObjectTree("Parent");
            assertNotNull("There should be a Record with the ID 'Parent'", fullTree);
            assertNotNull("The record Parent should have child-records", fullTree.getChildren());
            assertEquals("The record Parent should have 2 child-records", 2, fullTree.getChildren().size());

            assertNotNull("There should be a list of child-IDs", fullTree.getChildIds());
            assertEquals("There should be 2 child-IDs", 2, fullTree.getChildIds().size());
        } finally {
            storage.close();
            Thread.sleep(200); // Wait for freeing of resources
        }
    }

    public void testRelativesQueryOptions() throws Exception {
        DatabaseStorage storage = createStorageWithParentChild();
        try {
            QueryOptions qo = new QueryOptions(null, null, 10, 10);
            qo.setAttributes(QueryOptions.ATTRIBUTES_ALL);
            Record fullTree = storage.getRecord("Parent", qo);
            assertNotNull("There should be a Record with the ID 'Parent'", fullTree);
            assertNotNull("The record Parent should have child-records", fullTree.getChildren());
            assertEquals("The record Parent should have 2 child-records", 2, fullTree.getChildren().size());

            assertNotNull("There should be a list of child-IDs", fullTree.getChildIds());
            assertEquals("There should be 2 child-IDs", 2, fullTree.getChildIds().size());
        } finally {
            storage.close();
            Thread.sleep(200); // Wait for freeing of resources
        }
    }

    /**
     * Tests that requesting a Record with children that was previously connected but with severed connections
     * does not return the children as part of the Record tree.
     */
    // TODO: The logic of this test in unclear. Rethink it
    public void disablestestRelativesNoLongerRelated() throws Exception {
        DatabaseStorage storage = createStorageWithParentChild();
        try {
            {
                QueryOptions qo = new QueryOptions(null, null, 10, 10);
                qo.setAttributes(QueryOptions.ATTRIBUTES_ALL);
                Record fullTree = storage.getRecord("Parent", qo);
                assertNotNull("There should be a Record with the ID 'Parent'", fullTree);
                assertNotNull("The record Parent should have child-records", fullTree.getChildren());
                assertEquals("The record Parent should have 2 child-records", 2, fullTree.getChildren().size());

                assertNotNull("There should be a list of child-IDs", fullTree.getChildIds());
                assertEquals("There should be 2 child-IDs", 2, fullTree.getChildIds().size());

                Record child1 = null;
                for (Record child: fullTree.getChildren()) {
                    if ("Child1".equals(child.getId())) {
                        child1 = child;
                    }
                }
                fullTree.setChildIds(Collections.<String>emptyList());
                storage.flush(fullTree);
            }

            {
                Record pruned = storage.getRecord("Parent", null);
                assertNotNull("Record with the ID 'Parent' should still be available", pruned);
                assertFalse("The record Parent still have a  child-record",
                           pruned.getChildren() == null || pruned.getChildren().isEmpty());
                assertEquals("The Record Parent should now only have 1 child", 1, pruned.getChildren().size());
            }
        } finally {
            storage.close();
            Thread.sleep(200); // Wait for freeing of resources
        }
    }

    private DatabaseStorage createStorageWithParentChild() throws Exception {
        return createStorageWithParentChild(0);
    }
    private DatabaseStorage createStorageWithParentChild(int intermediateRecords) throws Exception {

        Configuration conf = createConf();
        conf.set(DatabaseStorage.CONF_RELATION_TOUCH, DatabaseStorage.RELATION.child);
        conf.set(DatabaseStorage.CONF_RELATION_CLEAR, DatabaseStorage.RELATION.parent);
        conf.set(DatabaseStorage.CONF_EXPAND_RELATIVES_ID_LIST, true);

        // We only want the IDs stored directly in the Record
        conf.set(DatabaseStorage.CONF_EXPAND_RELATIVES_ID_LIST, false);
        conf.set(H2Storage.CONF_H2_SERVER_PORT, 8079+storageCounter++);
        DatabaseStorage storage = new H2Storage(conf);

        try {
            Record parent = new Record("Parent", "dummy", new byte[0]);
            Record child1 = new Record("Child1", "dummy", new byte[0]);
            child1.setParentIds(Collections.singletonList("Parent"));
            storage.flushAll(Arrays.asList(parent, child1));

            for (int i = 0 ; i < intermediateRecords ; i++) {
                storage.flush(new Record("intermediate_" + i, "dummy", new byte[0]));
            }

            Record child2 = new Record("Child2", "dummy", new byte[0]);
            child2.setParentIds(Collections.singletonList("Parent"));
            storage.flush(child2);
        } catch (Exception e) {
            storage.close();
            fail("Unable to create storage with 1 parent, 2 children and " + intermediateRecords + " intermediates: " +
                 e.getMessage());
        }
        return storage;
    }

    public void testRelativesScalingContract() throws Exception {
        testRelativesScaling(true);
    }
    public void testRelativesScalingNoContract() throws Exception {
        testRelativesScaling(false);
    }

    private final int M = 1000000;
    /**
     * Performance test for relation-heavy Records.
     */
    private void testRelativesScaling(boolean obeyTimestampContract) throws Exception {
        final int RECORDS = 10000;
        final int PARENT_EVERY = 1000;
        final int LOG_EVERY = RECORDS/100;
        final int BATCH_SIZE= 100; // Way above aviser's 15

        final byte[] EMPTY = new byte[0];
        final Profiler profiler = new Profiler(1000, 1000);

        Configuration conf = createConf(); // ReleaseHelper.getStorageConfiguration("RelationsTest");
        // Extremely important (factor 1000) for performance
        conf.set(DatabaseStorage.CONF_RELATION_TOUCH, DatabaseStorage.RELATION.child);
        conf.set(DatabaseStorage.CONF_RELATION_CLEAR, DatabaseStorage.RELATION.parent);
        conf.set(DatabaseStorage.CONF_OBEY_TIMESTAMP_CONTRACT, obeyTimestampContract);
        conf.set(H2Storage.CONF_H2_SERVER_PORT, 8099);
        DatabaseStorage storage = new H2Storage(conf);
        try {
            storage.clearBase(testBase1);
            final RecordWriter writer = new RecordWriter(storage, BATCH_SIZE, 1000);

            String lastParent = null;
            for (int i = 0; i < RECORDS; i++) {
                Record record;
                if (i % PARENT_EVERY == 0) {
                    lastParent = "parent_" + i;
                    record = new Record(lastParent, testBase1, EMPTY);
                    record.setId(lastParent);
                } else {
                    record = new Record("child_" + i, testBase1, EMPTY);
                    record.setParentIds(lastParent == null ? null : Arrays.asList(lastParent));
                }
//            storage.flush(record);
                writer.processRecord(record);
                profiler.beat();
                if (i % LOG_EVERY == 0 || i == RECORDS - 1) {
                    log.info(String.format(Locale.ROOT, "Record %6d. Current / overall speed: %6.2f / %6.2f records/sec",
                                           i, profiler.getBps(false), profiler.getBps(true)));
                }
            }
            writer.flush();
            assertEquals("There should be the right amount of Records in storage at the end",
                         RECORDS, count(storage, testBase1));
            storage.clearBase(testBase1);
        } finally {
            storage.close();
        }
    }

    private List<Record> getRecordsWithParents(DatabaseStorage storage, String base) throws IOException {
        List<Record> records = new ArrayList<>();

        final QueryOptions options = new QueryOptions(null, null, 0, 1);
        options.setAttributes(QueryOptions.ATTRIBUTES_ALL);
        options.removeAttribute(QueryOptions.ATTRIBUTES.CHILDREN);
//        options.removeAttribute(QueryOptions.ATTRIBUTES.PARENTS);
        final long iteratorKey = storage.getRecordsModifiedAfter(0, base, options);

        Record record;
        try {
            while ((record = storage.next(iteratorKey)) != null) {
                records.add(record);
            }
        } catch (NoSuchElementException e) {
            // Expected (yes, it is a horrible signal mechanism)
        }
        return records;
    }

    private int count(DatabaseStorage storage, String base) throws IOException {

        final QueryOptions options = new QueryOptions();
        options.setAttributes(QueryOptions.ATTRIBUTES_ALL);
        options.removeAttribute(QueryOptions.ATTRIBUTES.CHILDREN);
        options.removeAttribute(QueryOptions.ATTRIBUTES.PARENTS);
        final long iteratorKey = storage.getRecordsModifiedAfter(0, base, options);

        int count = 0;
        try {
            while (storage.next(iteratorKey) != null) {
                count++;
            }
        } catch (NoSuchElementException e) {
            // Expected (yes, it is a horrible signal mechanism)
        }
        return count;
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
        final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);

        log.debug("Testing for " + records + " records");
        storage.clearBase(BASE);
        assertBaseCount(BASE, 0);

        for (int i = 0 ; i < records ; i++) {
            storage.flush(new Record("id" + i, BASE, DATA));
        }

        assertBaseCount(BASE, records);
    }

    public void testGetRecordsModifiedAfterPartial() throws Exception {
        final int BULK_SIZE = 20000;

        final String BASE = "baseAfter";
        final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);

        storage.clearBase(BASE);

        for (int i = 0 ; i < BULK_SIZE ; i++) {
            storage.flush(new Record("id_first_" + i, BASE, DATA));
        }
        Thread.sleep(500);
        storage.flush(new Record("id_middle", BASE, DATA));
        Thread.sleep(500);
        for (int i = 0 ; i < BULK_SIZE ; i++) {
            storage.flush(new Record("id_last_" + i, BASE, DATA));
        }

        Record middle = storage.getRecord("id_middle", null);
        long middleMTime = middle.getModificationTime();

        assertBaseCount("baseAfter", BULK_SIZE+1, middleMTime-1);
        assertBaseCount("baseAfter", BULK_SIZE, middleMTime);
    }

    public void testGetRecordsModifiedAfterEdge() throws Exception {
        final String BASE = "baseAfter";
        final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);

        storage.clearBase(BASE);
        storage.flush(new Record("id_A", BASE, DATA));
        Thread.sleep(10);
        storage.flush(new Record("id_B", BASE, DATA));

        long timeA = storage.getRecord("id_A", null).getModificationTime();
        long timeB = storage.getRecord("id_B", null).getModificationTime();

//        assertBaseCount("baseAfter", 2, 0);
//        assertBaseCount("baseAfter", 1, timeA);
        assertBaseCount("baseAfter", 0, timeB+1000);
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

    public void testGetRecordNoParentResolve() throws IOException {
        StringMap meta = new StringMap();
        addRecord("P1", "C1");
        addRecord("C1");
        QueryOptions opts = new QueryOptions(null, null, 10, 0, meta);

        {
            Record parent = storage.getRecord("P1", opts);
            assertNotNull("Requesting with parent ID 'P1' should work", parent);
            assertEquals("The number of child records should match", 1, parent.getChildren().size());
        }

        {
            Record child = storage.getRecord("C1", opts);
            assertNotNull("Requesting with child ID 'C1' should work", child);
            assertNull("There should be no parent records", child.getParents());
        }
    }

    public void testGetChildWithParentDirect() throws Exception {
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

    public void testGetChildWithParentIteratorAll() throws Exception {
        final QueryOptions parents = new QueryOptions(false, false, 1, 1, null, QueryOptions.ATTRIBUTES_ALL);
        parents.addAttribute(QueryOptions.ATTRIBUTES.PARENTS);
        checkRelationsHelper(parents);
    }

    public void testGetChildWithParentIteratorParents() throws Exception {
        final QueryOptions all = new QueryOptions(false, false, 1, 1, null, QueryOptions.ATTRIBUTES_ALL);
        checkRelationsHelper(all);
    }

    public void testGetChildWithParentIteratorChildren() throws Exception {
        final QueryOptions children = new QueryOptions(false, false, 1, 1, null, QueryOptions.ATTRIBUTES_ALL);
        children.addAttribute(QueryOptions.ATTRIBUTES.CHILDREN);
        checkRelationsHelper(children);
    }

    public void testGetChildWithParentIteratorNone() throws Exception {
        final QueryOptions none = new QueryOptions(false, false, 0, 0, null, QueryOptions.ATTRIBUTES_ALL);
        none.removeAttribute(QueryOptions.ATTRIBUTES.CHILDREN);
        none.removeAttribute(QueryOptions.ATTRIBUTES.PARENTS);

        checkRelationsHelper(none);
    }

    private void checkRelationsHelper(QueryOptions qo) throws Exception {
        List<Record> records = getAllRecordsFromIteratedSample(qo);
        for (Record record: records) {
            if (testId1.equals(record.getId())) { // Parent
                if (hasAttribute(qo, QueryOptions.ATTRIBUTES.CHILDREN) && record.getChildren() == null) {
                    fail("Children were requested but parent did not have any " + qo);
                }
                if (!hasAttribute(qo, QueryOptions.ATTRIBUTES.CHILDREN) && record.getChildren() != null) {
                    fail("Children were not requested but parent did have some " + qo);
                }
            } else if (testId2.equals(record.getId())) { // Child
                if (hasAttribute(qo, QueryOptions.ATTRIBUTES.PARENTS) && record.getParents() == null) {
                    fail("Parents were requested but child did not have any " + qo);
                }
                if (!hasAttribute(qo, QueryOptions.ATTRIBUTES.PARENTS) && record.getParents() != null) {
                    fail("Parents were not requested but child did have some " + qo);
                }
            } else {
                fail("Encountered unexpected record with ID '" + record.getId() + "'");
            }
        }
    }

    private boolean hasAttribute(QueryOptions qo, QueryOptions.ATTRIBUTES wanted) {
        for (QueryOptions.ATTRIBUTES candidate: qo.getAttributes()) {
            if (wanted == candidate) {
                return true;
            }
        }
        return false;
    }

    private List<Record> getAllRecordsFromIteratedSample(QueryOptions queryOptions) throws Exception {
        {
            Record r1 = new Record(testId1, testBase1, testContent1);
            Record r2 = new Record(testId2, testBase1, testContent1);
            r2.setParentIds(Arrays.asList(r1.getId()));
            storage.flushAll(Arrays.asList(r1, r2));
        }
        try {
            long iteratorKey = storage.getRecordsModifiedAfter(0L, testBase1, queryOptions);
            return storage.next(iteratorKey, 10000); // We should get them all in one go
        } finally {
            storage.clearBase(testBase1);
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
            fail("flushAll of " + r2 + " should fail but did not");
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
            fail("Flushing of two Records referencing each other as children should fail");
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
            log.info(String.format(Locale.ROOT, "Records: %6d, children touched/sec: %4d",
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
            log.info(String.format(Locale.ROOT, "Ingesting %d records of size %dMB for a total of %dMB",
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
            log.info(String.format(Locale.ROOT, "Finished ingesting of %dMB. Getting child 0 mtime...",
                                   records * CONTENT.length / M));

            QueryOptions options = new QueryOptions();
            options.setAttributes(QueryOptions.ATTRIBUTES_SANS_CONTENT_AND_META);
            options.removeAttribute(QueryOptions.ATTRIBUTES.PARENTS);
            long oldChildMtime = storage.getRecord("Child_0", options).getLastModified();
            log.info("Got child 0 mtime. Touching parent...");
            final long ttime = System.nanoTime();
            storage.flush(TOP);
            final long ms = (System.nanoTime()-ttime)/1000000;
            log.info(String.format(Locale.ROOT, "Parent touched in %dms (%d child records/sec). Getting child 0 mtime...",
                                   ms, records * 60 / ms));
            long newChildMtime = storage.getRecord("Child_0", options).getLastModified();
            assertFalse("The MTime of Child_0 should be changed after parent touch", oldChildMtime == newChildMtime);
            return ms;
        } finally {
            storage.close();
        }
    }

    public void testTwoLevelCycleParent() throws IOException {
        Record r1 = new Record(testId1, testBase1, testContent1);
        r1.setParentIds(Arrays.asList(testId2));
        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setParentIds(Arrays.asList(testId1));
        log.info("testTwoLevelCycleParent: Flushing 2 Records, referencing each other as parents");
        try{
            storage.flushAll(Arrays.asList(r1, r2));
            fail("Flushing of two Records referencing each other as parents should fail");
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
            fail("Flushing of three Records with cyclic child-referencing should fail");
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
     * Test get __statistics__ object.
     * @throws Exception If error.
     */
    public void testGetStatistics() throws Exception {
        storage.flush(new Record(testId1, testBase1, testContent1));
        storage.flush(new Record(testId2, testBase2, testContent1));

        StringMap meta = new StringMap();
        meta.put("ALLOW_PRIVATE", "true");
        QueryOptions opts = new QueryOptions(null, null, 0, 0, meta);
        Record statistics = storage.getRecord("__statistics__", opts);
        String response = statistics.getContentAsUTF8();

        assertTrue("The result should contain '(all' but was\n'" + response + "'", response.contains("all("));

        log.info(response);
        // TODO assert equals
    }

    public void testSimpleRelationStatistics() throws Exception {
        createRelationTestData();

        assertEquals("Total relation rows: 7", storage.getRelationStats(false));
    }

    public void testExtendedRelationStatistics() throws Exception {
        createRelationTestData();
        {
            assertRelationTestData();
            Record parent5 = storage.getRecord("parent5", null);
            Record child5 = storage.getRecord("child5", null);
            // Although the records are marked as deleted, the relations are still there
            assertEquals("The number of children for parent5 should match", 1, parent5.getChildren().size());
            assertEquals("The number of parents for child5 should match", 1, child5.getParents().size());
        }

        String report = storage.getRelationStats(true);
        // TODO: Check why it is not "relations (either parent or child present): 8"
        for (String expected: new String[]{
                "records (all): 11",
                "records (non-deleted): 5",
                "relations (all): 11",
                "relations (either parent or child present): 7",
                "relations (distinct parentIDs): 7",
                "relations (distinct non-deleted parentIDs): 3",
                "relations (distinct childIDs): 7",
                "relations (distinct non-deleted childIDs): 2",
        }) {
            if (!report.contains(expected)) {
                fail("Expected the report to contain '" + expected + "' but it was\n" + report);
            }
        }
    }

    public void testClearRelations_NoneValid() throws IOException {
        createRelationTestData();
        assertRelationTestData();
        // parent4 and record4
        assertEquals("Clearing should remove the correct amount of relations",
                     3, storage.clearRelations(DatabaseStorage.REMOVAL_REQUIREMENT.none_valid));
        assertRelationTestData();
    }

    public void testClearRelations_OnlyParentValid() throws IOException {
        createRelationTestData();
        assertRelationTestData();
        assertEquals("Clearing should remove the correct amount of relations",
                     6, storage.clearRelations(DatabaseStorage.REMOVAL_REQUIREMENT.only_parent_valid));
        assertRelationTestData();
    }

    public void testClearRelations_OnlyChildValid() throws IOException {
        createRelationTestData();
        assertRelationTestData();
        assertEquals("Clearing should remove the correct amount of relations",
                     4, storage.clearRelations(DatabaseStorage.REMOVAL_REQUIREMENT.only_child_valid));
        assertRelationTestData();
    }

    public void testClearRelations_OnlyOneValid() throws IOException {
        createRelationTestData();
        assertRelationTestData();
        assertEquals("Clearing should remove the correct amount of relations",
                     7, storage.clearRelations(DatabaseStorage.REMOVAL_REQUIREMENT.only_one_valid));
        assertRelationTestData();
    }

    private void createRelationTestData() throws IOException {
        addRecord("parent1", "child1", "child2");
        addRecord("parent2", "child1", "child2", "child3", "child4"); // child4 will never exist, child3 will be marked as deleted
        addRecord("parent3", "child1"); // Parent will be marked as deleted
        addRecord("parent4", "child4"); // Parent will be marked as deleted, child will never exist
        addRecord("parent5", "child5"); // Child will be marked as deleted
        addRecord("parent6", "child6"); // Both parent & child will be fully removed from records
        addRecord("parent7", "child7"); // Both parent & child will be marked as deleted

        addRecord("child1");
        addRecord("child2");
        addRecord("child3"); // Will be marked as deleted
        // Note: No child4
        addRecord("child5"); // Will be marked as deleted
        addRecord("child6"); // Will be removed
        addRecord("child7"); // Will be marked as deleted

        markAsDeleted("parent3");
        markAsDeleted("parent4");
        markAsDeleted("parent7");

        markAsDeleted("child3");
        markAsDeleted("child5");
        markAsDeleted("child7");

        storage.deleteRecord("parent6");
        storage.deleteRecord("child6");
    }

    private void assertRelationTestData() throws IOException {
        final QueryOptions qo = new QueryOptions(null, null, 10, 10, null);
        
        Record parent1 = storage.getRecord("parent1", qo);
        Record parent2 = storage.getRecord("parent2", qo);
        Record parent3 = storage.getRecord("parent3", qo);
        Record parent4 = storage.getRecord("parent4", qo);
        Record parent6 = storage.getRecord("parent6", qo);
        Record parent7 = storage.getRecord("parent7", qo);

        assertEquals("The number of children for parent1 should match", 2, parent1.getChildren().size());
        if (parent2.getChildren().size() != 2 && parent2.getChildren().size() != 3) {
            fail("The number of children for parent2 should be 2 or 3, depending on relation clean up, but was " +
                 parent2.getChildren().size());
        }
        assertTrue("parent3 should be marked as deleted", parent3.isDeleted());
        // Depends on clean up status
        //assertEquals("The number of children for parent3 should match", 1, parent3.getChildren().size());
        assertTrue("parent4 should be marked as deleted", parent4.isDeleted());
        assertNull("parent6 should not exist", parent6);
        assertTrue("parent7 should be marked as deleted", parent7.isDeleted());

        Record child1 = storage.getRecord("child1", qo);
        Record child2 = storage.getRecord("child2", qo);
        Record child3 = storage.getRecord("child3", qo);
        Record child4 = storage.getRecord("child4", qo);
        Record child5 = storage.getRecord("child5", qo);
        Record child6 = storage.getRecord("child6", qo);
        Record child7 = storage.getRecord("child7", qo);

        if (child1.getParents().size() != 3 && child1.getParents().size() != 2) {
            fail("The number of parents for child1 should be 2 or 3, depending on relation clean up, but was " +
                 child1.getParents().size());
        }
        assertEquals("The number of parents for child2 should match", 2, child2.getParents().size());
        // Depends on clean status
        //assertEquals("The number of parents for child3 should match", 1, child3.getParents().size());
        assertTrue("child3 should be marked as deleted", child3.isDeleted());
        assertNull("child4 should not exist", child4);
        assertTrue("child5 should be marked as deleted", child5.isDeleted());
        assertNull("child6 should not exist", child6);
        assertTrue("child7 should be marked as deleted", child7.isDeleted());

        // No check for parent4 and child4 ad the result depends on whether relations has been cleared
    }


    private Record addRecord(String id, String... childIDs) throws IOException {
        Record r = new Record(id, "dummy", new byte[0]);
        if (childIDs.length > 0) {
            r.setChildIds(Arrays.asList(childIDs));
        }
        storage.flush(r);
        return r;
    }
    private void markAsDeleted(String id) throws IOException {
        Record r = new Record(id, "dummy", new byte[0]);
        r.setDeleted(true);
        storage.flush(r);
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
        storage.close();
        // Start storage on a old database file
        storage = (DatabaseStorage) StorageFactory.createStorage(conf);
        storage.flush(new Record(testId1, testBase1, testContent1));
        assertTrue(start < storage.getModificationTime(testBase1));
        storage.close();
    }

    public void testDumpDatabase() throws IOException {
        final Path DEST = Files.createTempDirectory("h2_dump_test");
        Files.delete(DEST);
        createRelationTestData();
        log.info(storage.dumpToFilesystem(DEST.toString(), true));
        log.info("Finished dumping test h2 to " + DEST);
    }
}
