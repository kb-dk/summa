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

import dk.statsbiblioteket.summa.storage.BaseStats;
import dk.statsbiblioteket.summa.storage.StorageTestBase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.util.StringMap;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;

import java.util.List;
import java.util.Arrays;

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

    DatabaseStorage storage;

    public void setUp() throws Exception {
        super.setUp();
        this.storage = (DatabaseStorage)super.storage;
    }

    public void testStatsOnEmptyStorage() throws Exception {
        List<BaseStats> stats = storage.getStats();
        assertTrue(stats.isEmpty());
    }

    public void testStatsOnSingleRecord() throws Exception {
        long storageStart = storage.getModificationTime(null);
        Thread.sleep(2); // To make sure we have a timestamp delta
        storage.flush(new Record(testId1, testBase1, testContent1));
        List<BaseStats> stats = storage.getStats();

        assertEquals(1, stats.size());

        BaseStats base = stats.get(0);
        assertEquals(testBase1, base.getBaseName());
        assertEquals(1, base.getIndexableCount());
        assertEquals(0, base.getDeletedCount());
        assertEquals(1, base.getTotalCount());
        assertEquals(1, base.getLiveCount());
        assertTrue("Base mtime must be updated, but " +
                   "base.getModificationTime() <= storageStart: " +
                   base.getModificationTime() + " <= " +
                   storageStart,
                   base.getModificationTime() > storageStart);
    }

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

    public void testStatsWithMixedStates() throws Exception {
        Record r1 = new Record(testId1, testBase1, testContent1);

        Record r2 = new Record(testId2, testBase1, testContent1);
        r2.setDeleted(true);

        Record r3 = new Record(testId3, testBase1, testContent1);
        r3.setIndexable(false);

        Record r4 = new Record(testId4, testBase1, testContent1);
        r4.setDeleted(true);
        r4.setIndexable(false);

        storage.flushAll(Arrays.asList(r1,r2,r3,r4));
        List<BaseStats> stats = storage.getStats();

        assertEquals(1, stats.size());

        BaseStats base = stats.get(0);
        assertEquals(testBase1, base.getBaseName());
        assertEquals(2, base.getIndexableCount());
        assertEquals(2, base.getDeletedCount());
        assertEquals(4, base.getTotalCount());
        assertEquals(1, base.getLiveCount());
    }

    public void testIllegalPrivateAccess() throws Exception {
        try {
            storage.getRecord("__holdings__", null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // Good
        }
    }

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

        System.out.println(xml);
    }
}

