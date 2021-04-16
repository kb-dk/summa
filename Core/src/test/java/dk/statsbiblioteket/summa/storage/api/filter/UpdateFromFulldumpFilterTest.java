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
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.object.PayloadBufferFilter;
import dk.statsbiblioteket.summa.common.filter.object.PushFilter;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 *  Unit tests for {@link UpdateFromFulldumpFilter}
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk")
public class UpdateFromFulldumpFilterTest  extends TestCase {
    Storage storage;
    UpdateFromFilldumpFilterTestClass filter;
    PayloadBufferFilter chain;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if(storage != null) {
            storage.close();
        }
    }

    public PayloadBufferFilter prepareFilterChain(ObjectFilter fullDumpFilter,
            Record... records) throws IOException {
        // Set up the source filter
        PushFilter source = new PushFilter(records.length+1, 2048);

        for (Record record : records) {
            Payload p = new Payload(record);
            source.add(p);
        }
        source.signalEOF();

        fullDumpFilter.setSource(source);

        RecordWriter writer = new RecordWriter(storage, 1, 10000);
        writer.setSource(fullDumpFilter);

        // Set up the endpoint filter
        PayloadBufferFilter buf = new PayloadBufferFilter(
                                                Configuration.newMemoryBased());
        // Connect filters
        buf.setSource(writer);
        return buf;
    }

    public void assertBaseCount(String base, long expected) throws Exception {
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> iter = new StorageIterator(storage, iterKey);
        long actual = 0;
        while (iter.hasNext()) {
            Record r = iter.next();
            if (!r.isDeleted() && r.isIndexable()) {
                actual++;
            }
        }

        assertEquals("The base '" + base +"' should contain the right "
                     + "number of records:", expected, actual);
    }

    public Storage createTestStorage() throws Exception {
        String dbLocation = "target"+File.separator+"summatest" + File.separator + "testDB";
        File dbFile = new File(dbLocation);
        if (dbFile.getParentFile().exists()) {
            Files.delete(dbFile.getParentFile());
        }
        assertFalse("The DB should be removed",
                    dbFile.getParentFile().exists());

        storage = StorageFactory.createStorage(Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, dbLocation,
                DatabaseStorage.CONF_CREATENEW, true,
                DatabaseStorage.CONF_FORCENEW, true));
        storage.clearBase("base");
        
        assertBaseCount("base", 0);
        return storage;
    }

    /**
     * Test how we fare with no bases and no connection configured
     * @throws Exception if thrown
     */
    public void testNoBases() throws Exception {
        Storage storage = createTestStorage();

        assertBaseCount("base1", 0);

        Record r1 = new Record("id1", "base1", "data".getBytes(StandardCharsets.UTF_8));
        Record r2 = new Record("id2", "base1", "data".getBytes(StandardCharsets.UTF_8));

        filter = new UpdateFromFilldumpFilterTestClass(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base1"));
        chain = prepareFilterChain(filter, r1, r2);

        
        while(chain.pump()) {
            // intended
        }
        chain.close(true);

        assertEquals(2, chain.size());

        assertBaseCount("base1", 2);
    }

    public void testInsertExtraRecord() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes(StandardCharsets.UTF_8));
        Record rec2 = new Record("id2", "base", "data".getBytes(StandardCharsets.UTF_8));
        storage.flush(rec1);
        assertBaseCount("base", 1);

        filter = new UpdateFromFilldumpFilterTestClass(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base"));
        chain = prepareFilterChain(filter, rec1, rec2);

        while(chain.pump()) {
            // intended
        }
        chain.close(true);  // why isn't this called via pump?

        assertEquals(2, chain.size());
        assertBaseCount("base", 2);
    }

    public void testSameFullDump() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes(StandardCharsets.UTF_8));
        Record rec2 = new Record("id2", "base", "data".getBytes(StandardCharsets.UTF_8));
        storage.flush(rec1);
        storage.flush(rec2);
        assertBaseCount("base", 2);

        filter = new UpdateFromFilldumpFilterTestClass(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base"));
        chain = prepareFilterChain(filter, rec1, rec2);

        //noinspection StatementWithEmptyBody
        while(chain.pump()) {
            // intended
        }
        chain.close(true); // why isn't this called via pump?
        
        assertEquals(2, chain.size());
        assertBaseCount("base", 2);
    }

    public void testMissingInDump() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes(StandardCharsets.UTF_8));
        rec1.setDeleted(false);
        rec1.setIndexable(true);
        Record rec2 = new Record("id2", "base", "data".getBytes(StandardCharsets.UTF_8));
        rec2.setDeleted(false);
        rec2.setIndexable(true);

        storage.flush(rec1);
        storage.flush(rec2);

        assertBaseCount("base", 2);

        filter = new UpdateFromFilldumpFilterTestClass(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base"));
        chain = prepareFilterChain(filter, rec1);

        //noinspection StatementWithEmptyBody
        while(chain.pump()) {
            // intended
        }
        chain.close(true); // why isn't this called via pump?

        assertEquals(1, chain.size());

        assertBaseCount("base", 1);
    }

    /**
     * Test only deletes of input base.
     * @throws Exception if error.
     */
    public void testDifferentBases() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base1", "data".getBytes(StandardCharsets.UTF_8));
        Record rec2 = new Record("id2", "base1", "data".getBytes(StandardCharsets.UTF_8));
        Record rec3 = new Record("id3", "base2", "data".getBytes(StandardCharsets.UTF_8));
        Record rec4 = new Record("id4", "base3", "data".getBytes(StandardCharsets.UTF_8));

        storage.flush(rec1);
        storage.flush(rec2);
        storage.flush(rec3);
        storage.flush(rec4);

        assertBaseCount("base1", 2);
        assertBaseCount("base2", 1);
        assertBaseCount("base3", 1);

        filter = new UpdateFromFilldumpFilterTestClass(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base1"));
        chain = prepareFilterChain(filter, rec1);

        //noinspection StatementWithEmptyBody
        while(chain.pump()) {
            // intended
        }
        chain.close(true); // why isn't this called via pump?

        assertEquals(1, chain.size());

        assertBaseCount("base1", 1);
        assertBaseCount("base2", 1);
        assertBaseCount("base3", 1);
    }

    public void testAlreadyDeletedPost() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base1", "data".getBytes(StandardCharsets.UTF_8));
        Record rec2 = new Record("id2", "base1", "data".getBytes(StandardCharsets.UTF_8));
        Record rec3 = new Record("id3", "base1", "data".getBytes(StandardCharsets.UTF_8));
        rec2.setDeleted(true);
        rec2.setIndexable(false);

        storage.flush(rec1);
        storage.flush(rec2);
        storage.flush(rec3);

        assertBaseCount("base1", 2);

        filter = new UpdateFromFilldumpFilterTestClass(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base1"));
        chain = prepareFilterChain(filter, rec1);

        // Getting records is moved to first call to next
        assertEquals(0, filter.getNumberOfReadRecords());

        //noinspection StatementWithEmptyBody
        while(chain.pump()) {
            // intended
        }
        chain.close(true); // why isn't this called via pump?

        assertEquals(1, chain.size());

        assertBaseCount("base1", 1);
    }

    // TODO insert testcase for
    // http://sourceforge.net/apps/trac/summa/changeset/2274

    
    private class UpdateFromFilldumpFilterTestClass
                                              extends UpdateFromFulldumpFilter {
        public UpdateFromFilldumpFilterTestClass(Storage storage,
                                                         Configuration config) {
            super(config);
            init(config, storage, storage);
        }

        public int getNumberOfReadRecords() {
            return ids.size();
        }
    }
}